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

## Alert System

Still Here includes a tiered alert system that activates when check-ins stop. No special permissions are required — alerts open your phone's built-in SMS app or dialer with everything pre-filled. You confirm and send. Nothing happens silently without you.

| Day | What triggers |
|-----|--------------|
| 2   | Gentle notification — *"We're thinking of you"* |
| 3–7 (configurable) | Notification with one-tap SMS button to your personal contact |
| 7   | Persistent notification with direct call and SMS buttons for the Belgian Crisis Line (0800 32 123) |
| 10  | Persistent notification with call button for 112 — **opt-in only, explicit setting required** |

The 112 escalation requires deliberate opt-in in Settings. A warning is shown before it can be enabled.

### Why no silent background SMS?
Android flags apps that send SMS silently as potentially harmful. More importantly, a human confirmation before a message goes to emergency services is the right design — this app shouldn't act without you.

## Roadmap

### v1.2 — Snooze / I'm Okay
A one-tap snooze for when you've missed check-ins but are safe — travelling, phone died, busy week. Silences alerts for a configurable period without resetting the streak.

### v1.3 — Trusted Contact Companion
Opt-in flow for the person receiving the alert, so they have context and know what to do when the notification arrives.

---

## Contributing

Open an issue or pull request. Keep it simple — this app is meant to stay small.

---

## License

MIT
