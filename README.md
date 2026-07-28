# AutoFor - Call Forwarding Scheduler for Android

[![GitHub License](https://img.shields.io/github/license/NA-Egypt/autofor)](LICENSE)
[![Android SDK](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![UI Framework](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)

**AutoFor** is an intuitive, automated call forwarding manager for Android built with Jetpack Compose. It allows users to schedule dynamic call forwarding rules by day, time, and target phone number using standard MMI carrier codes (`*21*<number>#` and `#21#`).

---

## 🌟 Key Features

- 📅 **Automated Scheduling**: Set up rules with specific days of the week, start times, and end times.
- ⚡ **Manual Execution**: Instantly activate or cancel call forwarding with one-tap controls.
- 🎨 **Modern Material 3 UI**: Clean dashboard built with Jetpack Compose and customizable dark/light themes.
- 🔔 **Background Notifications**: Stay informed when scheduled forwarding rules activate or deactivate.
- 📱 **Monogram Branding**: Custom adaptive launcher icons and cohesive UI identity.

---

## 🛠️ Built With

* **Kotlin** - Primary programming language
* **Jetpack Compose** - Declarative UI framework
* **Android Architecture Components** - ViewModel, Flow, StateFlow, AlarmManager & BroadcastReceiver
* **Material Design 3** - Modern Android UI components

---

## 📦 Requirements

* **Android 8.0 (API level 26)** or higher
* **SIM Card / Carrier**: Must support MMI standard unconditional call forwarding codes (`*21*` and `#21#`).
* **Phone Call Permission**: Required to execute carrier MMI dialing requests.

---

## 🚀 Getting Started

### Prerequisites

* [Android Studio Jellyfish | 2023.3.1](https://developer.android.com/studio) or newer
* JDK 17
* Android SDK 35

### Installation & Build

1. **Clone the repository**:
   ```bash
   git clone https://github.com/NA-Egypt/autofor.git
   cd autofor
   ```

2. **Open in Android Studio**:
   Open the root project directory in Android Studio.

3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on Device**:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!  
Feel free to check out the [issues page](https://github.com/NA-Egypt/autofor/issues).
