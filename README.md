# MarkPoint

[![Android SDK](https://img.shields.io/badge/Android%20SDK-37-blue.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%2C%20Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](https://kotlinlang.org)

**MarkPoint** is a modern, professional Android application designed for precision measurement, marking, and interval calculations. Whether you are working in woodworking, construction, engineering, layout design, or crafting, MarkPoint helps you calculate exact mark positions, equal intervals, and custom spacing with high accuracy across multiple measurement units.

---

## ✨ Key Features

- **Dual Calculation Modes**:
  - **Equal Marks Mode**: Divide any span between a start and end point into a specified number of equal intervals.
  - **Spacing Mode**: Calculate mark positions based on fixed spacing distances, supporting both *Fixed Spacing* (with optional endpoint adjustment) and *Exact Endpoint* strategies.
- **Multi-Unit Support**: Seamlessly convert and calculate in **Millimeters (mm)**, **Centimeters (cm)**, **Meters (m)**, **Inches (in)**, **Feet (ft)**, and **Yards (yd)**.
- **High-Precision Calculation Engine**: Powered by Java `BigDecimal` arithmetic for exact interval and position calculations without floating-point rounding errors.
- **History & Saved Calculations**: Automatically save calculation history, review past layouts, reload previous configurations, and manage saved items with DataStore persistence.
- **Customizable Settings**: Configure default measurement units, fraction/decimal rounding precision, and display preferences.
- **Modern Material 3 UI**: Clean, intuitive interface with edge-to-edge support, dynamic theming, and responsive layouts built with Jetpack Compose.

---

## 🛠️ Architecture & Tech Stack

MarkPoint is built following modern Android development best practices and Clean Architecture principles:

- **UI Layer**: 100% [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 design components, Navigation Compose, and ViewModel state management.
- **Domain Layer**: Pure Kotlin business logic (`MarkPointCalculator`, `MarkPointValidator`, `MeasurementFormatter`) ensuring testable and robust calculations.
- **Data & Persistence**: Kotlinx Serialization combined with Jetpack DataStore Preferences for reliable, asynchronous local storage.
- **Testing**: Comprehensive unit tests covering calculation precision, boundary validation, and formatting rules (`JUnit 4`, Compose UI testing).

---

## 📱 Screenshots & UI Tour

- **Calculator Screen**: Intuitive inputs for Start, End, Mode selection, Mark Count / Spacing, and unit selectors with instant visual breakdown of mark positions.
- **History Screen**: Access previously saved calculations, reload entries, or clear history.
- **Settings Screen**: Tailor default units, precision rounding, and app preferences.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Koala / Iguana or newer recommended)
- Android SDK 37 (Compile & Target SDK)
- JDK 11 or higher

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/MarkPoint.git
   ```
2. Open the project folder in Android Studio.
3. Sync Gradle and run the `:app` module on an emulator or physical Android device (minSdk 26).

---

## 🧪 Running Tests

To run the unit test suite:
```bash
./gradlew test
```

To run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
