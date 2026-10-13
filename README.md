# White List Checker 🌐

[![Android CI & Build](https://github.com/fireslav-sky/White-List-Checker/actions/workflows/build.yml/badge.svg)](https://github.com/fireslav-sky/White-List-Checker/actions)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**White List Checker** — современная Android-утилита для экспресс-диагностики сетевой доступности хостов и автоматического выявления режима сетевой изоляции («белых списков» / Whitelist).

Приложение помогает быстро определить: есть ли у вас полноценный доступ ко всей сети Интернет или интернет-провайдер / оператор связи перевёл соединение в режим доступа только к ограниченному перечню социально значимых сайтов и сервисов.

---

## 🚀 Ключевые проблемы и их решение

Классические HTTP-клиенты и чекеры часто выдают **ложноотрицательные** результаты из-за особенностей сетевого окружения и защит современных сайтов. White List Checker устраняет эти ограничения:

| Проблема стандартных чекеров | Как решено в White List Checker |
|------------------------------|---------------------------------|
| **`CertPathValidatorException`** при проверке сайтов банков и госсервисов, использующих сертификаты Национального удостоверяющего центра (Минцифры). | Для сетевого чекера факт получения сертификата и успешный TLS Handshake означает, что хост **доступен на транспортном уровне**. Ошибки доверия сертификату фиксируются информационно, не ломая вердикт доступности. |
| **`Too many follow-up requests` (бесконечные редиректы)** при отправке голых HTTP `HEAD`-запросов на Ozon, маркетплейсы и крупные сервисы. | Добавлен интеллектуальный HTTP-фолбэк с ограничением глубины редиректов (до 5) и корректной трактовкой любых 3xx/4xx кодов ответа как подтверждения связи с веб-сервером. |
| **Блокировки WAF / Cloudflare / Qrator** из-за отсутствия стандартных сетевых заголовков. | Запросы отправляются с реалистичным мобильным заголовком `User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) ... Chrome/...`. |
| **Зависания и фризы интерфейса** при обрывах связи. | Настроены строгие Connect/Read/Write таймауты (настраиваются в диапазоне 2–15 сек), асинхронный параллельный опрос через Kotlin Coroutines. |

---

## ✨ Возможности

- ⚡ **Параллельная диагностика**: одновременный опрос пула хостов с фиксацией точного RTT (Round-Trip Time) в миллисекундах.
- 🎯 **Интеллектуальный вердикт**:
  - 🟢 **Свободный интернет**: контрольные и зарубежные сервисы отвечают штатно.
  - 🟡 **Режим «белых списков»**: работают только российские реестровые ресурсы (Госуслуги, налоги, банки), а независимые контрольные узлы заблокированы.
  - 🟠 **Частичные ограничения**: часть сервисов недоступна либо наблюдаются потери пакетов и таймауты.
  - 🔴 **Нет подключения**: интерфейс сети отключен или отсутствует маршрут.
- 🗂️ **Категории ресурсов**:
  - **Белые списки**: Госуслуги, Сбер, Т-Банк, ВТБ, Мой Налог, Ozon и др.
  - **Контрольные узлы**: Wikipedia, GitHub, Cloudflare DNS (1.1.1.1), Google DNS (8.8.8.8), зарубежные CDN.
  - **Пользовательские сайты**: добавление любых собственных доменов и IP-адресов.
- 🎨 **Material You (Material Design 3)**:
  - Плавная поддержка динамических цветов (Dynamic Color) на Android 12+.
  - Горизонтальный скролл фильтров по категориям.
  - Удобный и крупный FAB для мгновенного добавления хостов.
  - Диалоговые окна подтверждения удаления для защиты от случайных нажатий.
  - Полная поддержка Edge-to-Edge и тёмной темы.
- 📋 **История и экспорт**:
  - Логирование предыдущих сессий проверки.
  - Экспорт форматированного текстового отчёта со всеми задержками и кодами для передачи провайдеру или коллегам.

---

## 🛠️ Стек технологий и архитектура

- **Язык**: [Kotlin](https://kotlinlang.org/) (2.x)
- **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- **Архитектура**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
- **Асинхронность**: Kotlin Coroutines & `StateFlow`
- **Сетевой стек**: [OkHttp 4](https://square.github.io/okhttp/) с настраиваемым `PermissiveTrustManager` и `HostnameVerifier`
- **Хранение данных**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (сохранение пользовательских хостов и настроек таймаутов)
- **Тестирование**: JUnit 4, AndroidX Test, Robolectric

---

## 📥 Сборка и запуск

### Требования
- **JDK**: 17 или новее
- **Android SDK**: `compileSdk = 36`, `minSdk = 26` (Android 8.0+)
- **Gradle**: 8.9+ (Gradle Wrapper включён в репозиторий)

### Клонирование репозитория
```bash
git clone https://github.com/fireslav-sky/White-List-Checker.git
cd white-list-checker
```

### Сборка Debug APK
```bash
./gradlew assembleDebug
```
Собранный файл будет находиться по пути:
`app/build/outputs/apk/debug/app-debug.apk`

### Запуск модульных тестов
```bash
./gradlew testDebugUnitTest
```

---

## 📦 Релизная сборка (GitHub Release)

### 1. Подготовка ключа подписи
Для сборки релизного APK создайте или укажите ваш keystore:
```bash
keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

### 2. Сборка Release APK
```bash
export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"

./gradlew assembleRelease
```
Файл APK будет сгенерирован в `app/build/outputs/apk/release/app-release.apk`.

### 3. Автоматическая сборка в GitHub Actions
Репозиторий содержит готовый workflow `.github/workflows/build.yml`.
При каждом коммите или создании тега вида `v1.0.0` GitHub Actions автоматически:
1. Прогоняет модульные тесты.
2. Собирает Debug APK.
3. Прикрепляет сгенерированный APK к артефактам сборки для моментальной загрузки.

---

## 📄 Лицензия

Проект распространяется под открытой лицензией **MIT License**. Подробнее см. в файле [LICENSE](LICENSE).
