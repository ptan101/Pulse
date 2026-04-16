# Pulse Android Application

## Description

Pulse is an Android application designed to interface with Bluetooth Low Energy (BLE) devices, providing a seamless and intuitive user experience for monitoring and managing BLE connections. Developed for Android 11, Pulse leverages advanced BLE capabilities to offer features such as real-time device scanning, connection management, and data visualization. This application is perfect for users looking to interact with BLE devices in a more meaningful way, whether for personal use, development, or testing.

## Installation and Setup

### Prerequisites

- Android Studio 4.0 or later
- JDK 11
- An Android device or emulator running Android 11 (API level 30) or later

### Cloning the Repository

To get started with the Pulse Android application, clone the repository to your local machine using the following command:

```bash
git clone https://github.com/yourusername/Pulse_Android11.git
cd Pulse_Android11
```

### Configuring the Project

1. Open the project in Android Studio by selecting "Open an Existing Project" and navigating to the cloned repository.
2. Once the project is open, check the `local.properties` file to ensure it points to your Android SDK location.
3. Use the `Sync Project with Gradle Files` button in Android Studio to resolve any missing dependencies and prepare the project for compilation.

## Running the Application

To run the application, follow these steps:

1. Connect your Android device to your computer or start an Android emulator.
2. In Android Studio, select your device/emulator as the target device from the dropdown near the run button.
3. Click the run button to build and run the application on your device/emulator.

## Dependencies

This project utilizes several key dependencies to manage BLE connections and data visualization:

- Android BLE API
- Google Gson for JSON parsing
- LiveData and ViewModel for reactive UI updates

Ensure these dependencies are included in your `build.gradle` file to avoid compilation issues.

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".

Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Key Java Functions

Within the `NotificationHandler.java`:

- **createNotificationChannel(Context context)**: Sets up a Notification Channel for app notifications, a requirement for Android Oreo and above.
- **makeNotification(String title, String text, Context context)**: Generates a standard notification with a title and text content.
- **makeNotification(int id, Notification notification)**: Overloads the makeNotification method to allow for ID-based notification management, useful for updating or canceling notifications.

Please review individual Java files for detailed descriptions of each function and their respective usages.

# Pulse BLE Core Module

This module handles the Bluetooth Low Energy (BLE) scanning, connection management, and real-time data parsing for custom wearable bio-sensors (e.g., multi-wavelength PPG arrays and ECG trackers). It is designed to run continuously in the background, decode custom packet structures dynamically, and push biometric data to the UI and remote Sickbay servers.

## 🏗️ Core Architecture

* **`BLEHandlerService`**: A foreground Android Service that acts as the central manager for all BLE operations. It handles scanning via `BluetoothLeScanner`, manages concurrent connections through a `TattooConnectionManager`, and routes data to the `SickbayPushService`.
* **`BLEDevice` (Base Class)**: Represents a connected sensor. It manages the connection state, logs connection/disconnection events to a `MarkerFile`, handles local data recording, and triggers a device-specific haptic vibration if the sensor drops offline unexpectedly.
* **`BLETattooDevice`**: Inherits from `BLEDevice` to specifically handle continuous biometric streams. It processes raw Nordic UART Service (NUS) and Custom UART Service (CUS) packets, runs biometric algorithms, and broadcasts `PlotDataEvent` and `SickbaySendFloatsEvent` payloads via EventBus.
* **`BLEPacketParser`**: A dynamic packet decoding engine. Instead of hardcoding data structures, it reads configuration rules from an `.init` file in the assets folder to determine signal order, byte size, endianness, signed/unsigned properties, and digital filter settings.
* **`DebugBLEDevice`**: A mock device implementation that simulates a BLE connection (`00:00:00:00:00:00d`). It generates synthetic waveforms (Sine, Square, Sawtooth) based on the target `.init` file configuration to allow for offline UI and pipeline testing.
* **`AlertStopReceiver`**: A `BroadcastReceiver` that captures the "Silence" action from the high-priority disconnect notification, allowing users to stop the haptic alarm without opening the app.

## ✨ Key Features

* **Dynamic Packet Parsing**: Instantiates a parser based on the device's Bluetooth name. Supports automatic bit-shifting, sign extension, and real-time data filtering.
* **High-Priority Disconnect Alarms**: If a sensor drops connection unintentionally, the service triggers a looping 700ms/500ms haptic vibration and fires a max-priority system notification. The alarm is intentionally bypassed if the user manually requests a disconnect.
* **Sickbay Integration**: Natively binds to `SickbayPushService` to map parsed float arrays to specific Sickbay IDs for live remote monitoring.
* **Event-Driven UI Updates**: Uses `EventBus` to decouple the background service from the UI, broadcasting `ScanListUpdatedEvent`, `PlotDataEvent`, and `DeviceConnectionChangedEvent` for smooth graphing.

## 🛠️ Adding a New Sensor

To add support for a new custom BLE sensor (like a new PPG array version):
1. Create an `.init` file in the `assets/inits/` directory defining the packet structure, notification frequency, and Sickbay IDs.
2. Add the device's Bluetooth identifier and the corresponding `.init` filename to the `assets/inits/init_file_lookup.txt` table.
3. The `BLEPacketParser` will automatically load the rules when the device connects.

## License

Distributed under the MIT License. See `LICENSE` for more information.