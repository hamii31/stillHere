# Still Here

A minimal Android app. One button. Press it every day.

That's it.

---

## What it does

**Still Here** is a daily check-in app. You open it, you press the button, it knows you're still here. It tracks your streak and total days checked in. No account. No internet. No notifications pestering you. Just you and a button.

The name says everything.

---

## Download

Grab the latest APK from the [Releases](../../releases) page and sideload it onto your Android device.

> **To install:** After downloading, tap the APK on your phone. If prompted, enable *Install from unknown sources* for your browser or file manager.

---

## Features

- Single large check-in button
- Tracks current streak and total days
- Fully offline — no data leaves your device
- No account required
- Dark theme

---

## Build from source

1. Clone the repo
2. Open in Android Studio
3. Let Gradle sync
4. Run on an emulator or physical device

Requires Android 7.0 (API 24) or higher.

---

## Roadmap

### v1.1 — Emergency Contact Alerts *(planned)*
If the button goes unpressed for a set number of days (default: 7), the app will automatically alert a designated contact — either a close person or an emergency number. The goal is a quiet safety net: if something is wrong and you can't press the button, someone will know.

Planned implementation:
- User-configurable grace period (e.g. 3, 5, or 7 days)
- Option to set a personal contact or a crisis/emergency number
- SMS or call trigger after the grace period expires
- A simple "I'm okay, just busy" snooze option to silence the alert without resetting the streak

---

## Contributing

Open an issue or pull request. Keep it simple — this app is meant to stay small.

---

## License

MIT
