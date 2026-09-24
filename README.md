# HADashboard 🏠

An Android App & Home Screen Widget (built with **Jetpack Compose** & **Jetpack Glance**) for controlling and monitoring **Home Assistant** in a single unified dashboard block.

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue)
![Jetpack Glance](https://img.shields.io/badge/UI-Jetpack%20Glance-orange)

---

## Features

- **Live Camera Snapshot (66% Width x 66% Height)**:
  - Fetches live camera frames via Home Assistant REST API with one-tap refresh button 🔄.
- **3 Sensor Tiles (33% Width x 66% Height)**:
  - Stacked vertically on 3 lines (e.g. Solar, Grid, Battery).
  - Displays big bold values with customizable units (`V`, `W`, `kW`, `°C`, `%`, etc.).
- **4 Action Buttons (100% Width x 33% Height)**:
  - Interactive toggle buttons at the bottom for relays, switches, lights, or scenes.
  - Active/Inactive color state feedback with compact indicator.
- **Configuration Screen (`MainActivity`)**:
  - Configure Server URL, Long-Lived Access Token, Camera Entity ID, 3 Sensor Entity IDs & Units, and 4 Button Entity IDs.
  - Test connection directly from the app.

---

## Tech Stack

- **UI**: Jetpack Compose (App Settings) & Jetpack Glance (Home Screen Widget)
- **Networking**: OkHttp (Home Assistant REST API `/api/states`, `/api/services`, `/api/camera_proxy`)
- **Persistence**: SharedPreferences (`HaPreferences`)
- **Asynchronous Flow**: Kotlin Coroutines (`async` parallel fetching)

---

## Setup & Usage

1. Open the **HADashboard** app on your Android device.
2. Enter your **Home Assistant Server URL** (e.g., `http://192.168.1.100:8123` or Nabu Casa URL).
3. Paste a **Long-Lived Access Token** (generated from your HA Profile -> Security -> Long-Lived Access Tokens).
4. Configure Entity IDs for Camera, 3 Sensors, and 4 Buttons.
5. Tap **Save & Verify Connection**.
6. Add the **HADashboard** 4x2 widget to your Home Screen!

---

## License

MIT License
