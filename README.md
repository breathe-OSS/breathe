# Breathe (Android)

> _"Breathe, breathe in the air. Don't be afraid to care."_ - **Pink Floyd**, _The Dark Side of the Moon_

<p align="center">
  <img src="assets/logo.png" alt="App Icon" width="128"/>
</p>

<p align="center">
  <img src="assets/screenshot-1.png" alt="Screenshot 1" width="200" style="border-radius:26px;"/>
  <img src="assets/screenshot-2.png" alt="Screenshot 2" width="200" style="border-radius:26px;"/>
  <img src="assets/screenshot-3.png" alt="Screenshot 3" width="200" style="border-radius:26px;"/>
</p>

**Breathe** is a modern, MD3 Android application designed to monitor real-time Air Quality Index (AQI) levels across Jammu & Kashmir. Built with **Kotlin** and **Jetpack Compose**, it provides a clean, fluid interface to track pollution levels using the Indian National Air Quality Index (NAQI) standards.

- Check the [**breathe api**](https://github.com/breathe-OSS/api?tab=readme-ov-file#how-the-aqi-is-calculated) repo to know how the AQI is calculated.

## Features

- **Material Design 3 Expressive**
- **AMOLED Dark Theme Support**
- **Supports devices running Android 8.1 and above**
- **Fluid Animations and interactive UI**
- **Advanced animation options**
- **AQI Spectrum**
- **Real-time Monitoring**
- **US and Indian AQI standards**
- **Detailed Breakdown**
- **Cigarette equivalence**
- **A map with data laid across**
- **24 Hour graph of AQI Data**
- **Widget Support**

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material3)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit & Gson
- **Concurrency:** Kotlin Coroutines & Flow
- **Theme:** Dynamic Material You (Monet)

## Structure

The project follows a standard but modularized android app structure.

```app/src/main/java/com/sidharthify/breathe/
├── data/    
│   ├── AnimationSettings.kt
│   ├── BreatheApi.kt
│   └── Models.kt
├── navigation/
│   └── Navigation.kt
├── ui/                    # All UI related code
│   ├── components/        # Reusable UI elements (Cards, Graphs, Dashboards)
│   │   ├── CardComponents.kt
│   │   ├── DashboardComponents.kt
│   │   └── GraphComponents.kt
│   │   └── MorphingPill.kt
│   └── screens/           # Full-page screen composables
│       ├── ExploreScreen.kt
│       ├── HomeScreen.kt
│       ├── MapScreen.kt
│       └── SettingsScreen.kt
├── util/                  # Helper functions and extensions
│   └── Utils.kt
│   └── IndiaBoundaryOverlay.kt
├── viewmodel/             # Caching and loading
│   └── BreatheViewModel.kt
├── widgets/               # Widget logic
│   ├── BreatheWidget.kt
│   ├── BreatheWidgetWorker.kt
│   └── WidgetActions.kt
└── MainActivity.kt        # Application Entry Point
```

## Build and deploy locally

### Prerequisites

- Android Studio Hedgehog or newer.
- JDK 17.
- A running instance of the **Breathe Backend** (Python/FastAPI).

### Installation

1. **Clone the repository:**
   `git clone https://github.com/breathe-OSS/breathe && cd breathe`

2. **Open in Android Studio.**

- Alternatively, if you have the command line launcher configured: `studio .`

3. **Configure the API Endpoint:**

- Open `app/src/main/java/com/sidharthify/breathe/data/BreatheApi.kt`.
- Update `BASE_URL` to point to your backend server (e.g., your local IP if running locally).

4. **Build and Run:**

- GUI: Sync Gradle files, select your device, and click Run.
- Terminal: Ensure your device is connected (`adb devices`) and run: `./gradlew installDebug`

## AQI Data Providers

### Why this exists

Publicly available AQI data for the Jammu & Kashmir region is currently unreliable. Most standardized sources rely on sparse sensor networks or algorithmic modeling that does not accurately reflect ground-level realities. This results in widely varying values across different platforms. **Google**, for example, shows values that are insanely **low**, but they are usually off by a huge margin.

**Breathe** aims to solve this by strictly curating sources and building a ground-truth network.

The method that we use to convert the raw data in our API **(please do read the documentation)** was laid out by scanning past concentration trends from 2025-2022 of the J&K regions.

## Current Data Sources

### Open-Meteo

Used for all pollutant values for **most regions** in Jammu & Kashmir (excluding Srinagar and Jammu).
Open-Meteo's satellite-based air quality model provides stable and consistent values that generally fall within the expected range of nearby ground measurements.

- Air quality & pollutant data: [Open-Meteo Air Quality API](https://open-meteo.com/en/docs/air-quality-api)

- Weather forecasts & historical data: [Open-Meteo](https://open-meteo.com)

### AirGradient

Used for the **Srinagar** and **Jammu** region, where the sensors are deployed in real time.

- Their website: [AirGradient](https://www.airgradient.com/)

This provides accurate values of PM10 and PM2.5. Other values are fetched from Open-Meteo (like O₃ and NO₂)

## Call for Contributors (Hardware)

The limitations of our current project is that we do not have ground sensors in every region and are mostly relying on satellite data, so the data is **not 100%** accurate.

We are actively working to deploy custom physical sensors to improve data density in Jammu. If you are interested in hosting a sensor node, please contact us at: [wednisegit@gmail.com](mailto:wednisegit@gmail.com)

We have deployed two **AirGradient** sensors in Jammu and Srinagar which provide an accurate measurement of PM10 and PM2.5 values. We are working
to deploy them in two other regions; Kishtwar and Reasi.

## Credits & Developers

This project is fully Free & Open Source Software (FOSS).

## Built by:

1. [sidharthify](https://github.com/sidharthify) (Lead Dev)
2. [Flashwreck](https://github.com/Flashwreck) (Co-lead and Devops maintainer)
