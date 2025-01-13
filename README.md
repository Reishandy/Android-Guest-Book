# Android-Guest-Book
This repository contains the Android client application for the guest book system paired with the FastAPI guest book application. It allows users to check-in by scanning QR codes or manually entering their ID, import and export guest data via CSV files, and reset check-in statuses.

# Features
- Check-in:
  - Users can check-in by either scanning a QR code or manually entering their ID.
  - Real-time updates and notifications upon successful check-in.

- Import CSV:
  - Import guest data from CSV files.
  - Validate the CSV content before importing.

- Export CSV:
  - Export guest data to CSV files.
  - Save the exported file to a selected location on the device.

- Reset Check-in:
  - Reset the check-in status of a specific ID or all entries.
  - Confirmation dialog to prevent accidental resets.

- QR Code Scanner:
  - Built-in QR code scanner using CameraX.
  - Real-time QR code detection and processing.


# Installation
1. Clone the repository:
```bash
git clone https://github.com/Reishandy/Android-Guest-Book.git
cd Android-Guest-Book
```

2. Open the project in Android Studio:
  - Open Android Studio.
  - Click on File > Open... and select the project directory.

# Usage
1. Run the application:
  - Connect an Android device or start an emulator.
  - Click on Run > Run 'app'.

2. Access the application:
-   The application will start on the connected device or emulator.

# Release
Latest Release: [v1](https://github.com/Reishandy/Android-Guest-Book/releases/tag/v1)
- Assets: [Guest.Book.apk](https://github.com/Reishandy/Android-Guest-Book/releases/download/v1/Guest.Book.apk)
- Release Notes: Signed APK

# Paired Server
This Android guest book application is paired with an FastAPI server application, which can be found here: [FastAPI-Guest-Book](https://github.com/Reishandy/FastApi-Guest-Book).

# Contributing
1. Fork the repository.
2. Create a new branch (git checkout -b feature-branch).
3. Commit your changes (git commit -am 'Add new feature').
4. Push to the branch (git push origin feature-branch).
5. Create a new Pull Request.

# License
This project is licensed under the AGPL-3.0 License. See the [LICENSE](LICENSE) file for details.
