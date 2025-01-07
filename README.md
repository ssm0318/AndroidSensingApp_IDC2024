# IDC 2024 AppMinder

## Overview
This application is designed to track the usage statistics of a user's phone. It records data such as application usage, notifications, device usage, etc. The data is saved in the Firebase Realtime Database and local storage in JSON format. The app is designed to be efficient and robust in data tracking and transmission.

## File Structure
```
.
├── ApiManager.kt
├── AppTrackWorker.kt
├── AppUsageMonitorService.kt
├── BootBroadcastReceiver.kt
├── FileSaveWorker.kt
├── FloatingButtonService.kt
├── MainActivity.kt
├── NotificationReceiver.kt
├── NotificationService.kt
├── TrackerService.kt
├── UploadWholeWorker.kt
├── Utils.kt
├── model
│   └── sensors
│       ├── NotificationModel.kt
│       └── UsageStatsModel.kt
└── tracker
    ├── NotificationListener.kt
    ├── Tracker.kt
    ├── TrackerInterface.kt
    └── UsageStatsTracker.kt
```

----

## How to Use
1. Clone the project.
2. Open the project in Android Studio.
3. Ensure you have a Firebase project set up and have added this Android project to it.
4. Build and run the project.

## Notes
### 1. Permissions

The application requires several permissions to function correctly:

- **Overlay permission**: This permission is required to display the floating button over other applications. It's essential for Android versions Marshmallow (6.0) and above.

- **Notification access**: This permission is required for the `NotificationListenerService` to access and interact with the notifications posted on the status bar. It allows the app to gather data about notifications as they are posted or removed.

- **Usage stats access**: This permission is required for accessing usage statistics data from the `UsageStatsManager` system service. The app uses this data to track device usage.

### 2. Floating Button

The floating button is an interactive UI element that overlays on top of other applications. When clicked, it expands to show four different buttons, each representing a specific emotion quadrant: [Low Energy, Low Pleasantness], [Low Energy, High Pleasantness], [High Energy, Low Pleasantness], and [High Energy, High Pleasantness]. The user can select the button that best represents their current emotional state. This data can then be used for emotion tracking and analysis.

### 3. Usage Stats and Notifications Tracking

The application uses the `UsageStatsTracker` and `NotificationListener` to track device usage statistics and notifications. This data includes the time spent on each application and details about each notification posted or removed from the status bar. The collected data is stored locally and also sent to a Firebase Realtime Database for remote access and analysis. 

### 4. Reboot Upon Restart

The app features a reboot upon restart mechanism. This means that the app's tracking services, such as the `UsageStatsTracker` and `NotificationListener`, will automatically restart after the device reboots. This ensures continuous data collection without any manual intervention from the user.

### 5. Social Media Usage Tracking and Reminders

Users can select their most-used social media application from a drawer within the app. The app then tracks usage of the selected application and sends reminders to open the application. Notifications are sent when the app hasn't been used for a certain duration, encouraging regular engagement with the social media platform.

### 6. Daily Reflection Notifications

The application sends daily notifications for reflections at 8:30 PM. These notifications remind the user to reflect on their day and submit their reflections within the app. Reflection can include their mood, activities, and thoughts about the day.

### 7. EMA Questions

The main screen of the app features EMA (Ecological Momentary Assessment) questions that users can respond to. Upon submission, the responses are sent to the Firebase Realtime Database for further analysis. The questions are reset after each submission, enabling users to answer them multiple times a day.

----

## Detailed Explanation of Important Files

### ApiManager.kt
This singleton object is responsible for writing and uploading data to the Firebase Realtime Database. It retrieves and uploads data in real-time based on a user's unique ID. The object also includes a callback mechanism to retry uploading data in case of failure.

### Utils.kt
This file contains utility functions like converting Unix time to human-readable time and vice versa, creating notification channels, and checking if the application has the necessary permissions.

### Tracker.kt
This abstract class is a blueprint for any tracker objects created in this application. It provides the essential start and stop functions for a tracker.

### NotificationListener.kt
This class is a service that listens for notifications posted or removed from the status bar. It stores these events and their details into the `NotificationListAdapter`.

### UsageStatsTracker.kt
This class is responsible for fetching usage statistics of applications on the device and storing them into `UsageStatsListAdapter`. The period of fetching the stats is determined by the `days` parameter in the class constructor.

### ModelInterface.kt & ModelAdapterInterface.kt
These are interfaces for data model and adapter objects. They ensure a consistent structure for the data collected by the trackers and the list adapters used to store and manage this data.

### NotificationModel.kt
This data class represents a notification posted or removed by an application. The corresponding `NotificationListAdapter` is used to manage a queue of these objects.

### UsageStatsModel.kt
This data class represents usage statistics for a certain package. The corresponding `UsageStatsListAdapter` is used to manage a queue of these objects.
