# Stimulation Player

This is a single-purpose Android application designed for a specific device class (Amlogic S905X3, Android 9) to play a local video file while synchronously executing a stimulation script described in a JSON file.

## Features

*   **Local Media Playback**: Plays a user-selected local video file.
*   **Scripted Events**: Synchronizes playback with a JSON-based script to trigger events.
*   **Audio Cues**: Provides metronome clicks, step transition beeps, and sequence transition beeps.
*   **Text Overlay**: Displays instructional notes from the script over the video.
*   **Fullscreen Mode**: Supports a distraction-free fullscreen mode, toggled by a double-tap.
*   **Robust Validation**: Includes strict validation of the JSON script to ensure correct execution.

## How to Build and Install

### Prerequisites

*   Android Studio
*   An Android device running Android 9 (API 28) or higher

### Building the APK

1.  Clone this repository.
2.  Open the project in Android Studio.
3.  Let Gradle sync and download the required dependencies.
4.  From the menu, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5.  Once the build is complete, you can find the generated APK file in the `app/build/outputs/apk/debug/` directory.

### Installation

You can install the APK on your target device using Android Debug Bridge (ADB):

```bash
adb install /path/to/app-debug.apk
```

## How to Use

### Placing Files

Before using the app, ensure you have a video file and a corresponding JSON script file accessible on your device's local storage.

### Running a Session

1.  **Launch the App**: Open the "Stimulation Player" application.
2.  **Select Video**:
    *   Tap the **Select** button next to the "Video:" label.
    *   Use the file picker to navigate to and choose your desired video file.
3.  **Select Script**:
    *   Tap the **Select** button next to the "Script:" label.
    *   Use the file picker to navigate to and choose the corresponding JSON script file.
4.  **Validation**:
    *   The application will automatically validate the selected JSON script.
    *   If the script is valid and a video is selected, the **Play** button will become enabled.
    *   If the script is invalid, a toast message will appear with a descriptive error, and the **Play** button will remain disabled.
5.  **Start Playback**:
    *   Tap the **Play** button to begin the synchronized playback session. The video will start, and the script's events (audio cues, overlay text) will execute in sync with the video's timeline.
6.  **Stop Playback**:
    *   During playback, the **Stop** button is enabled. Tap it at any time to end the session. This will stop the video and all script events, resetting the app to a ready state with the files still selected.
7.  **Toggle Fullscreen**:
    *   During playback, double-tap anywhere on the video surface to enter fullscreen mode. This hides all UI controls, showing only the video and the overlay text.
    *   Double-tap again to exit fullscreen mode and bring the UI controls back.

## Playback Behavior

*   **No Pause/Seek**: The application does not have pause or seek functionality. A session is a single, continuous playback.
*   **Session Reset**: Stopping a session or navigating away from the app (e.g., pressing the home button) will reset the playback state.
*   **Playback End**: When the script timeline is complete, all audio and overlay events will cease. The video may continue to play to its end, depending on its length.
