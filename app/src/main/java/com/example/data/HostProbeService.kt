package com.example.data

import android.os.SystemClock
import com.example.model.CheckStep
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.HostStatus
import com.example.model.StepResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class HostProbeService {

    companion object {
        const val CONNECT_TIMEOUT_MS = 5000
        const val READ_TIMEOUT_MS = 5000
        const val MAX_REDIRECTS = 3
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    }

    /**
     * Специальный TrustManager для задач сетевой проверки доступности.
     * Принимает любые сертификаты, поскольку сам факт получения сертификата
     * от сервера на этапе TLS доказывает физическую доступность хоста и порта 443.
     * Это устраняет ложноотрицательные ошибки (CertPathValidatorException)
     * для ресурсов с сертификатами Минцифры РФ (Сбер, ВТБ, Госуслуги и др.).
     */
    private val networkTestTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private val permissiveSslSocketFactory: SSLSocketFactory by lazy {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(networkTestTrustManager), SecureRandom())
        sslContext.socketFactory
    }

    /**
     * Интерцептор для мягкого ограничения редиректов (не более 3).
     * Если WAF/антифрод (например, Ozon) отдаёт цикличные редиректы,
     * интерцептор возвращает последний 3xx ответ, не бросая исключение
     * "Too many follow-up requests: 21", так как ответ 3xx доказывает доступность хоста.
     */
    private class BoundedRedirectInterceptor(private val maxRedirects: Int = MAX_REDIRECTS) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var response = chain.proceed(request)
            var redirectCount = 0

            while (response.isRedirect && redirectCount < maxRedirects) {
                val location = response.header("Location") ?: break
                val newUrl = response.request.url.resolve(location) ?: break
                response.close()
                redirectCount++
                request = request.newBuilder().url(newUrl).build()
                try {
                    response = chain.proceed(request)
                } catch (e: Exception) {
                    // Если при редиректе произошёл сбой, возвращаем исходное подтверждение ответа
                    break
                }
            }
            return response
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .callTimeout((CONNECT_TIMEOUT_MS + READ_TIMEOUT_MS + 2000).toLong(), TimeUnit.MILLISECONDS)
            .sslSocketFactory(permissiveSslSocketFactory, networkTestTrustManager)
            .hostnameVerifier { _, _ -> true }
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(BoundedRedirectInterceptor(MAX_REDIRECTS))
            .build()
    }

    suspend fun checkHost(rawHost: String, group: HostGroup): HostCheckResult = withContext(Dispatchers.IO) {
        val cleanHost = cleanHostName(rawHost)
        val stepResults = mutableListOf<StepResult>()
        var totalLatency = 0L

        // ==========================================
        // ШАГ 1: DNS-РЕЗОЛВ
        // ==========================================
        val dnsStart = SystemClock.elapsedRealtime()
        var resolvedAddresses: Array<InetAddress>? = null
        var dnsErrorType: HostStatus? = null
        var dnsErrorMessage: String? = null

        try {
            val resolved = withTimeoutOrNull(CONNECT_TIMEOUT_MS.toLong()) {
                InetAddress.getAllByName(cleanHost)
            }
            if (resolved != null && resolved.isNotEmpty()) {
                resolvedAddresses = resolved
            } else {
                dnsErrorType = HostStatus.TIMEOUT_ERROR
                dnsErrorMessage = "Превышено время ожидания DNS (> ${CONNECT_TIMEOUT_MS} мс)"
            }
        } catch (e: UnknownHostException) {
            dnsErrorType = HostStatus.NETWORK_ERROR
            dnsErrorMessage = "Домен не найден (NXDOMAIN) или нет связи"
        } catch (e: Exception) {
            dnsErrorType = classifyException(e)
            dnsErrorMessage = e.localizedMessage ?: e.javaClass.simpleName
        }

        val dnsLatency = SystemClock.elapsedRealtime() - dnsStart
        totalLatency += dnsLatency

        if (resolvedAddresses == null || resolvedAddresses.isEmpty()) {
            val status = dnsErrorType ?: HostStatus.NETWORK_ERROR
            val msg = dnsErrorMessage ?: "Не удалось разрешить IP-адрес хоста"
            stepResults.add(
                StepResult(
                    step = CheckStep.DNS,
                    isSuccess = false,
                    latencyMs = dnsLatency,
                    details = "Сбой DNS: $msg"
                )
            )
            return@withContext HostCheckResult(
                host = cleanHost,
                group = group,
                status = status,
                totalLatencyMs = totalLatency,
                failedStep = CheckStep.DNS,
                errorMessage = msg,
                stepResults = stepResults
            )
        }

        val primaryAddress = resolvedAddresses[0]
        val ipSummary = resolvedAddresses.take(3).joinToString(", ") { it.hostAddress ?: "" }
        stepResults.add(
            StepResult(
                step = CheckStep.DNS,
                isSuccess = true,
                latencyMs = dnsLatency,
                details = "IP: $ipSummary"
            )
        )

        // ==========================================
        // ШАГ 2: TCP-ПОДКЛЮЧЕНИЕ НА ПОРТ 443
        // ==========================================
        val tcpStart = SystemClock.elapsedRealtime()
        var tcpSocket: Socket? = null
        var tcpErrorType: HostStatus? = null
        var tcpErrorMessage: String? = null

        try {
            val socket = Socket()
            socket.soTimeout = READ_TIMEOUT_MS
            socket.connect(InetSocketAddress(primaryAddress, 443), CONNECT_TIMEOUT_MS)
            tcpSocket = socket
        } catch (e: SocketTimeoutException) {
            tcpErrorType = HostStatus.TIMEOUT_ERROR
            tcpErrorMessage = "Таймаут подключения TCP (> ${CONNECT_TIMEOUT_MS} мс)"
        } catch (e: ConnectException) {
            tcpErrorType = HostStatus.NETWORK_ERROR
            tcpErrorMessage = "Соединение сброшено / отказ в подключении (RST)"
        } catch (e: NoRouteToHostException) {
            tcpErrorType = HostStatus.NETWORK_ERROR
            tcpErrorMessage = "Нет маршрута к узлу (No route to host)"
        } catch (e: Exception) {
            tcpErrorType = classifyException(e)
            tcpErrorMessage = e.localizedMessage ?: e.javaClass.simpleName
        }

        val tcpLatency = SystemClock.elapsedRealtime() - tcpStart
        totalLatency += tcpLatency

        if (tcpSocket == null || !tcpSocket.isConnected) {
            val status = tcpErrorType ?: HostStatus.NETWORK_ERROR
            val msg = tcpErrorMessage ?: "Отказ в соединении"
            stepResults.add(
                StepResult(
                    step = CheckStep.TCP,
                    isSuccess = false,
                    latencyMs = tcpLatency,
                    details = "Сбой TCP 443: $msg"
                )
            )
            return@withContext HostCheckResult(
                host = cleanHost,
                group = group,
                status = status,
                totalLatencyMs = totalLatency,
                failedStep = CheckStep.TCP,
                errorMessage = msg,
                stepResults = stepResults
            )
        }

        stepResults.add(
            StepResult(
                step = CheckStep.TCP,
                isSuccess = true,
                latencyMs = tcpLatency,
                details = "Порт 443 открыт (${primaryAddress.hostAddress})"
            )
        )

        // ==========================================
        // ШАГ 3: TLS-РУКОПОЖАТИЕ (ТРАНСПОРТНЫЙ УРОВЕНЬ)
        // ==========================================
        val tlsStart = SystemClock.elapsedRealtime()
        var sslSocket: SSLSocket? = null
        var tlsErrorType: HostStatus? = null
        var tlsErrorMessage: String? = null
        var tlsProtocol: String? = null
        var tlsCipher: String? = null

        try {
            val ssl = permissiveSslSocketFactory.createSocket(tcpSocket, cleanHost, 443, true) as SSLSocket
            ssl.soTimeout = READ_TIMEOUT_MS
            ssl.startHandshake()
            tlsProtocol = ssl.session.protocol
            tlsCipher = ssl.session.cipherSuite
            sslSocket = ssl
        } catch (e: SocketTimeoutException) {
            tlsErrorType = HostStatus.TIMEOUT_ERROR
            tlsErrorMessage = "Таймаут рукопожатия TLS (> ${READ_TIMEOUT_MS} мс)"
        } catch (e: SSLHandshakeException) {
            // Если ошибка всё же связана с цепочкой сертификатов, сам факт ответа сервера
            // сертификатом подтверждает успешное прохождение сетевого уровня TLS!
            val causeMsg = e.cause?.localizedMessage ?: e.localizedMessage ?: ""
            if (causeMsg.contains("CertPathValidatorException", ignoreCase = true) ||
                causeMsg.contains("Trust anchor", ignoreCase = true) ||
                causeMsg.contains("certificate", ignoreCase = true)
            ) {
                // Сервер ответил своим сертификатом — на сетевом уровне TLS доступен!
                tlsProtocol = "TLS (сертификат получен)"
                tlsCipher = "Сертификат передан сервером"
            } else {
                tlsErrorType = HostStatus.NETWORK_ERROR
                tlsErrorMessage = "Сброс TLS-сессии: ${e.localizedMessage ?: "ошибка рукопожатия"}"
            }
        } catch (e: SSLException) {
            tlsErrorType = HostStatus.NETWORK_ERROR
            tlsErrorMessage = "Сбой SSL/TLS: ${e.localizedMessage ?: "разрыв соединения"}"
        } catch (e: Exception) {
            tlsErrorType = classifyException(e)
            tlsErrorMessage = e.localizedMessage ?: e.javaClass.simpleName
        }

        val tlsLatency = SystemClock.elapsedRealtime() - tlsStart
        totalLatency += tlsLatency

        if (tlsProtocol == null) {
            try { tcpSocket.close() } catch (_: Exception) {}
            val status = tlsErrorType ?: HostStatus.NETWORK_ERROR
            val msg = tlsErrorMessage ?: "Ошибка TLS рукопожатия"
            stepResults.add(
                StepResult(
                    step = CheckStep.TLS,
                    isSuccess = false,
                    latencyMs = tlsLatency,
                    details = "Сбой TLS: $msg"
                )
            )
            return@withContext HostCheckResult(
                host = cleanHost,
                group = group,
                status = status,
                totalLatencyMs = totalLatency,
                failedStep = CheckStep.TLS,
                errorMessage = msg,
                stepResults = stepResults
            )
        }

        stepResults.add(
            StepResult(
                step = CheckStep.TLS,
                isSuccess = true,
                latencyMs = tlsLatency,
                details = "$tlsProtocol (${tlsCipher ?: "OK"})"
            )
        )

        // Закрываем сокеты перед HTTP-запросом через OkHttp
        try { sslSocket?.close() } catch (_: Exception) {}
        try { tcpSocket.close() } catch (_: Exception) {}

        // ==========================================
        // ШАГ 4: HTTP ЗАПРОС (ПРИКЛАДНОЙ УРОВЕНЬ)
        // ==========================================
        val httpStart = SystemClock.elapsedRealtime()
        var httpCode: Int? = null
        var httpProtocolString: String? = null
        var httpStatusMessage: String? = null
        var httpErrorType: HostStatus? = null
        var httpErrorMessage: String? = null

        try {
            // Используем стандартный GET без чтения тела (эквивалентен по трафику HEAD,
            // но не блокируется антифрод-системами/WAF, которые отвергают чистый HEAD)
            val request = Request.Builder()
                .url("https://$cleanHost/")
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Connection", "close")
                .build()

            val response = okHttpClient.newCall(request).execute()
            httpCode = response.code
            httpProtocolString = response.protocol.toString().uppercase()
            httpStatusMessage = response.message
            response.close()
        } catch (e: SocketTimeoutException) {
            httpErrorType = HostStatus.TIMEOUT_ERROR
            httpErrorMessage = "Таймаут ожидания HTTP-ответа (> ${READ_TIMEOUT_MS} мс)"
        } catch (e: IOException) {
            httpErrorType = HostStatus.NETWORK_ERROR
            httpErrorMessage = "Сетевой разрыв при HTTP: ${e.localizedMessage ?: e.javaClass.simpleName}"
        } catch (e: Exception) {
            httpErrorType = classifyException(e)
            httpErrorMessage = e.localizedMessage ?: e.javaClass.simpleName
        }

        val httpLatency = SystemClock.elapsedRealtime() - httpStart
        totalLatency += httpLatency

        // Любой полученный HTTP-код (2xx, 3xx редирект, 4xx WAF/авторизация, 5xx сервер)
        // доказывает, что веб-сервер ответил на сетевой запрос!
        if (httpCode == null) {
            val status = httpErrorType ?: HostStatus.NETWORK_ERROR
            val msg = httpErrorMessage ?: "Сервер не вернул HTTP ответ"
            stepResults.add(
                StepResult(
                    step = CheckStep.HTTP,
                    isSuccess = false,
                    latencyMs = httpLatency,
                    details = "Сбой HTTP: $msg"
                )
            )
            return@withContext HostCheckResult(
                host = cleanHost,
                group = group,
                status = status,
                totalLatencyMs = totalLatency,
                failedStep = CheckStep.HTTP,
                errorMessage = msg,
                stepResults = stepResults
            )
        }

        val responseDesc = formatHttpCodeDescription(httpCode, httpStatusMessage)
        stepResults.add(
            StepResult(
                step = CheckStep.HTTP,
                isSuccess = true,
                latencyMs = httpLatency,
                details = "HTTP $httpCode $responseDesc ($httpProtocolString)"
            )
        )

        // Итоговый статус хоста
        val finalStatus = if (totalLatency > 2000L) HostStatus.SLOW else HostStatus.AVAILABLE

        return@withContext HostCheckResult(
            host = cleanHost,
            group = group,
            status = finalStatus,
            totalLatencyMs = totalLatency,
            failedStep = null,
            errorMessage = null,
            stepResults = stepResults
        )
    }

    private fun formatHttpCodeDescription(code: Int, message: String?): String {
        return when (code) {
            in 200..299 -> "OK"
            301, 308 -> "Постоянный редирект"
            302, 307 -> "Временный редирект"
            in 300..399 -> "Редирект"
            401, 403 -> "Ограничение доступа (WAF/Auth, узел отвечает)"
            404 -> "Не найдено (узел отвечает)"
            429 -> "Лимит запросов (узел отвечает)"
            in 400..499 -> "Клиентская ошибка (узел отвечает)"
            in 500..599 -> "Ответ сервера (5xx)"
            else -> message ?: ""
        }
    }

    private fun classifyException(e: Throwable): HostStatus {
        return when (e) {
            is SocketTimeoutException, is TimeoutException -> HostStatus.TIMEOUT_ERROR
            is ConnectException, is UnknownHostException, is NoRouteToHostException, is SSLException -> HostStatus.NETWORK_ERROR
            else -> {
                val msg = e.message?.lowercase() ?: ""
                if (msg.contains("timeout") || msg.contains("timed out")) {
                    HostStatus.TIMEOUT_ERROR
                } else {
                    HostStatus.NETWORK_ERROR
                }
            }
        }
    }

    private fun cleanHostName(input: String): String {
        var clean = input.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        val slashIndex = clean.indexOf('/')
        if (slashIndex != -1) {
            clean = clean.substring(0, slashIndex)
        }
        val colonIndex = clean.indexOf(':')
        if (colonIndex != -1) {
            clean = clean.substring(0, colonIndex)
        }
        return clean
    }
}
