# Hotspot Optimizer

Simple Android utility that helps reduce interference and distractions while using mobile hotspot.

## What it actually does

- Turns **Bluetooth OFF**
- Enables **Do Not Disturb** (mutes notifications)
- Shows current status of Bluetooth and DND
- Quick button to open system App settings (so you can force-stop background apps yourself)
- Keeps the screen awake while the app is open (helps some phones keep the hotspot more stable)

## What it does **NOT** do

- Does **not** increase Wi-Fi transmit power
- Does **not** improve signal range or strength
- Cannot force-stop other apps automatically (Android restriction)

## How to get the APK

1. Go to the **Actions** tab of this repository
2. Wait for the build to finish
3. Download the artifact named `app-debug`
4. Extract and install `app-debug.apk` on your phone (allow "Install from unknown sources")

## Permissions used

- `BLUETOOTH` / `BLUETOOTH_CONNECT` – to turn Bluetooth off
- `ACCESS_NOTIFICATION_POLICY` – to control Do Not Disturb (you must grant this manually the first time)
