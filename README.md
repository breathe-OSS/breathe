# Breathe (Android)

> *"Breathe, breathe in the air. Don't be afraid to care."* - **Pink Floyd**, *The Dark Side of the Moon*

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

* **Material Design 3 Expressive**
* **Real-time Monitoring**
* **Indian NAQI Standards**
* **Detailed Breakdown**
* **A map with data laid across**
* **24 Hour graph of AQI Data**

## Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Networking:** Retrofit & Gson
* **Concurrency:** Kotlin Coroutines & Flow
* **Theme:** Dynamic Material You (Monet)

## Getting Started

### Prerequisites

* Android Studio Hedgehog or newer.
* JDK 17.
* A running instance of the **Breathe Backend** (Python/FastAPI).

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/breathe-OSS/breathe

3. **Open in Android Studio.**
4. **Configure the API Endpoint:**
  - Open `app/src/main/java/com/sidharthify/breathe/BreatheApi.kt`.
  - Update `BASE_URL` to point to your backend server (e.g., your local IP if running locally).
4. **Build and Run:**
  - Sync Gradle files.
  - Select your device and click Run.

## AQI Data Providers
### Why this exists
Publicly available AQI data for the Jammu & Kashmir region is currently unreliable. Most standardized sources rely on sparse sensor networks or algorithmic modeling that does not accurately reflect ground-level realities. This results in widely varying values across different platforms. **Google**, for example, shows values that are insanely **low**, but they are usually off by a huge margin.

**Breathe** aims to solve this by strictly curating sources and building a ground-truth network.

The method that we use to convert the raw data in our API **(please do read the documentation)** was laid out by scanning past concentration trends from 2025-2022 of the J&K regions.

### Current Data Sources
**Open-Meteo**

Used for most regions in Jammu & Kashmir (excluding Srinagar).
Open-Meteo's satellite-based air quality model provides stable and consistent values that generally fall within the expected range of nearby ground measurements.

Air quality & pollutant data: Open-Meteo Air Quality API - https://open-meteo.com/en/docs/air-quality-api

Weather forecasts & historical data: https://Open-Meteo.com


**OpenAQ (CPCB Mirror)**

Used for the Srinagar region, where government ground sensor data is available.

Data publisher: Central Pollution Control Board (CPCB) – https://cpcb.gov.in/

Data access platform: OpenAQ – https://openaq.org

This provides official measured pollutant concentrations where available.

### Call for Contributors (Hardware)
The limitations of our current project is that we do not have ground sensors and are mostly relying on satellite data, so the data is **not 100%** accurate.


We are actively working to deploy custom physical sensors to improve data density in Jammu. If you are interested in hosting a sensor node, please contact us at: [wednisegit@gmail.com](mailto:wednisegit@gmail.com)

We have emailed **AQI.in** (an Indian company with local ground community sensors) and **Caeli** (satellite based) for use of their API and free research plans if any.

## Credits & Developers
This project is fully Free & Open Source Software (FOSS).

## Built by:
1. [sidharthify](https://github.com/sidharthify) (Lead Dev)
2. [Flashwreck](https://github.com/Flashwreck) (Devops maintainer and designer)
