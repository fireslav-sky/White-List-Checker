package com.fireslav.whitelistchecker.model

/**
 * Чистая (без Android-зависимостей) логика вынесения вердикта по результатам проверки.
 */
object VerdictEvaluator {

    fun isAvailable(status: HostStatus): Boolean {
        return status == HostStatus.AVAILABLE || status == HostStatus.SLOW
    }

    fun evaluate(results: List<HostCheckResult>): Verdict {
        val whitelist = results.filter { it.group == HostGroup.WHITELIST }
        val control = results.filter { it.group == HostGroup.CONTROL }

        val whiteTotal = whitelist.size
        val whiteSuccess = whitelist.count { isAvailable(it.status) }

        val controlTotal = control.size
        val controlSuccess = control.count { isAvailable(it.status) }

        // 0. Вердикт строится на сравнении двух групп. Если одна из них пуста,
        // сравнивать не с чем — иначе пустая группа считалась бы «полностью недоступной».
        if (whiteTotal == 0 || controlTotal == 0) {
            val missing = if (whiteTotal == 0 && controlTotal == 0) {
                "нет ни белых, ни контрольных хостов"
            } else if (whiteTotal == 0) {
                "нет хостов из белого списка"
            } else {
                "нет контрольных хостов"
            }
            return Verdict(
                title = "Недостаточно данных для вердикта",
                description = "Для определения режима белых списков нужны хосты обеих групп, а в списке $missing. Добавьте их в настройках.",
                type = VerdictType.PENDING
            )
        }

        val whiteRatio = whiteSuccess.toFloat() / whiteTotal
        val controlRatio = controlSuccess.toFloat() / controlTotal

        // 1. Почти все «белые» доступны, почти все контрольные недоступны:
        if (whiteRatio >= 0.70f && controlRatio <= 0.30f) {
            return Verdict(
                title = "Похоже, включён режим белых списков",
                description = "Большинство ресурсов из белого списка доступны ($whiteSuccess из $whiteTotal), а контрольные внешние сервисы заблокированы ($controlSuccess из $controlTotal).",
                type = VerdictType.WHITELIST_ACTIVE
            )
        }

        // 2. Доступно всё:
        if (whiteRatio >= 0.70f && controlRatio >= 0.70f) {
            return Verdict(
                title = "Ограничений не обнаружено",
                description = "Доступны как российские сервисы ($whiteSuccess из $whiteTotal), так и контрольные зарубежные ресурсы ($controlSuccess из $controlTotal).",
                type = VerdictType.NO_RESTRICTIONS
            )
        }

        // 3. Недоступно почти всё:
        if (whiteRatio <= 0.30f && controlRatio <= 0.30f) {
            return Verdict(
                title = "Нет связи или полная блокировка",
                description = "Практически ни один сервис не отвечает ($whiteSuccess/$whiteTotal белых, $controlSuccess/$controlTotal контрольных). Проверьте интернет-соединение.",
                type = VerdictType.NO_CONNECTION
            )
        }

        // 4. Смешанный результат:
        return Verdict(
            title = "Смешанный результат",
            description = "Частичная доступность сервисов ($whiteSuccess/$whiteTotal белых, $controlSuccess/$controlTotal контрольных). Возможно, действуют нестабильная связь или выборочные сбои.",
            type = VerdictType.MIXED
        )
    }
}
