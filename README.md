# QueueFree - Smart Queue Management System

QueueFree is an Android application designed to reduce physical queues at service centers. Users can generate digital tokens, view real-time queue status, find nearby centers, scan QR codes, and receive token-related notifications.

## Features

- User registration and login using Firebase Authentication
- Role-based access for users and administrators
- Digital token generation for selected service centers
- Real-time queue and token status tracking
- FIFO-based queue management
- QR code generation and scanning for token verification
- GPS-based nearby service center discovery
- Push notifications for token updates
- Admin dashboard to manage tokens and queue status

## Technologies Used

- Android Studio
- Java
- Firebase Authentication
- Firebase Realtime Database
- Firebase Cloud Messaging
- Google Maps API
- Google Location Services
- ZXing QR Scanner
- Gradle

## Application Flow

1. Users register or log in using Firebase Authentication.
2. The app checks the user role and opens the User Dashboard or Admin Dashboard.
3. Users select a service center and generate a digital token.
4. Token details and queue position are stored in Firebase Realtime Database.
5. Users can track their queue position in real time.
6. Administrators call the next token and update its status.
7. Users receive notifications when their token is called or completed.
8. QR code scanning can be used to verify a token.

## Project Structure

```text
QueueFree/
├── app/
│   ├── src/main/java/        # Java source files
│   ├── src/main/res/         # Layouts, icons, strings, and other resources
│   ├── AndroidManifest.xml   # App permissions and component configuration
│   ├── build.gradle.kts      # App-level Gradle configuration
│   └── google-services.json  # Firebase configuration
├── build.gradle.kts          # Project-level Gradle configuration
├── settings.gradle.kts       # Project modules and repositories
├── gradlew                   # Gradle Wrapper for Linux/macOS
└── gradlew.bat               # Gradle Wrapper for Windows
```

## Setup Instructions

1. Clone the repository:

```bash
git clone [https://github.com/your-username/QueueFree.git](https://github.com/your-username/QueueFree.git)
```

2. Open the project in Android Studio.

3. Create a Firebase project in the Firebase Console.

4. Add an Android app to Firebase using your application package name.

5. Download the `google-services.json` file and place it inside the `app/` folder.

6. Enable the required Firebase services:
   - Firebase Authentication
   - Firebase Realtime Database
   - Firebase Cloud Messaging

7. Add your Google Maps API key in `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY" />
```

8. Sync Gradle and run the application on an Android device or emulator.

## Required Permissions

The application may require the following permissions:

- Internet access for Firebase services
- Location access for nearby service center discovery
- Camera access for QR code scanning
- Notification permission for token updates

## Future Enhancements

- Online payment integration
- Estimated waiting-time calculation
- Appointment booking
- Multi-language support
- Analytics dashboard for administrators
- Separate dashboards for different service-center departments

## Author

Your Name  
GitHub: [your-github-profile](https://github.com/your-username)

## License

This project is developed for educational purposes.
