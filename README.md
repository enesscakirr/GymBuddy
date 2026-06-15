# GymBuddy

An Android social fitness app that lets users track workouts, connect with nearby gym-goers via Bluetooth/Location, and message each other in real time.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Navigation | Navigation Compose |
| Backend | Firebase Auth + Firestore |
| Auth | Google Sign-In (Credential Manager) |
| Image Loading | Coil |
| Local Storage | DataStore Preferences |
| Async | Kotlin Coroutines + Flow |
| Location | Google Play Services Location |

---

## Architecture

```
com.example.gymbuddy/
│
├── data/                        # Data layer
│   ├── model/                   # Domain models (User, WorkoutSession, ChatMessage, Conversation)
│   ├── preferences/             # DataStore wrapper (AppPreferences)
│   └── repository/              # Firebase data access (Auth, User, Workout, Chat)
│
├── navigation/                  # Single-activity nav graph (AppNavigation)
│
└── ui/                          # Presentation layer
    ├── components/              # Shared composables (BottomNavBar)
    ├── screens/
    │   ├── auth/                # Login · Register · CompleteProfile
    │   ├── chat/                # Conversation list · Chat thread
    │   ├── connect/             # Nearby users (BLE / Location)
    │   ├── home/                # Workout tracker
    │   ├── main/                # Root scaffold + bottom nav host
    │   ├── profile/             # Profile · Edit · Goals · History · Settings
    │   └── scan/                # QR / barcode scan
    ├── theme/                   # Color · Typography · Theme
    └── viewmodel/               # AuthVM · ChatVM · ConnectVM · WorkoutVM · SettingsVM
```

---

## Features

- **Authentication** — Email/password and Google Sign-In via Credential Manager
- **Workout Tracker** — Log exercises, sets, and reps in real time
- **Workout History** — Browse and review past sessions
- **Nearby Connect** — Discover other users at the gym via Bluetooth / location
- **Messaging** — Real-time 1-on-1 chat powered by Firestore
- **Profile & Goals** — Set fitness goals, upload profile photo, edit personal info
- **Settings** — Theme and account preferences via DataStore

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- A Firebase project with **Authentication** and **Firestore** enabled

### Setup

1. Clone the repo and open in Android Studio.
2. Place your `google-services.json` inside `app/`.
3. Enable **Email/Password** and **Google** sign-in methods in Firebase Console.
4. Run on a device or emulator with API 26+.

---

## Requirements

- Min SDK: **26** (Android 8.0)
- Target SDK: **35**
- Compile SDK: **35**
