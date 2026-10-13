package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.model.NetworkStatusInfo
import com.example.model.NetworkType

class NetworkStatusHelper(private val context: Context) {

    fun getCurrentNetworkStatus(): NetworkStatusInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkStatusInfo(
                networkType = NetworkType.NO_NETWORK,
                isVpnActive = false,
                networkDescription = "Служба сети недоступна"
            )

        val activeNetwork = connectivityManager.activeNetwork
            ?: return NetworkStatusInfo(
                networkType = NetworkType.NO_NETWORK,
                isVpnActive = false,
                networkDescription = "Сеть не подключена"
            )

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return NetworkStatusInfo(
                networkType = NetworkType.NO_NETWORK,
                isVpnActive = false,
                networkDescription = "Нет данных о подключении"
            )

        val isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }

        val description = when (networkType) {
            NetworkType.CELLULAR -> "Мобильная сеть оператора (сотовые данные)"
            NetworkType.WIFI -> "Wi-Fi сеть (локальное подключение)"
            NetworkType.ETHERNET -> "Проводная сеть Ethernet"
            NetworkType.NO_NETWORK -> "Нет активного подключения к интернету"
            NetworkType.OTHER -> "Другое сетевое подключение"
        }

        return NetworkStatusInfo(
            networkType = networkType,
            isVpnActive = isVpn,
            networkDescription = description
        )
    }
}
