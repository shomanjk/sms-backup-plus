# Changelog

All notable changes to **this unofficial experimental fork**
([shomanjk/sms-backup-plus](https://github.com/shomanjk/sms-backup-plus))
are documented in this file.

This project uses its own SemVer (`0.x` while experimental). Version numbers
here are **not** jberkel SMS Backup+ releases and are independent of henrichg
`1.7.0`.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Removed

- Google Play in-app Donate preference, BillingClient dependency, and donation
  activity (broken on this sideload fork; may return later as a fork funding link).

## [0.1.2] - 2026-07-14

`versionCode` 1804.

### Changed

- First-launch / upgrade no longer opens the full About page. Shows a short
  Welcome or What's new dialog instead; menu → About keeps credits and docs.
- About HTML updated for this fork (warnings, links, henrichg credit, release notes).

## [0.1.1] - 2026-07-14

`versionCode` 1803.

### Fixed

- Crash on startup on Android 12+ (API 31+): archived Firebase JobDispatcher
  `GooglePlayDriver` creates `PendingIntent`s without mutability flags. Force
  the AlarmManager scheduler before constructing `BackupJobs` on API 31+.

## [0.1.0] - 2026-07-14

`versionCode` 1802.

Unofficial maintained fork. Not affiliated with jberkel or henrichg. **Not ready
for daily use** — automatic backup still relies on archived Firebase
JobDispatcher until WorkManager.

**Based on:** [jberkel/sms-backup-plus](https://github.com/jberkel/sms-backup-plus)
`master` @ `ef97bfa9` (includes RCS / group MMS threading work past the last
Play release `1.5.11`; tree was labeled `1.6.0-BETA2`). Ports functional Android
7+ backup trigger/receiver fixes from
[henrichg/sms-backup-plus](https://github.com/henrichg/sms-backup-plus).

### Added

- Experimental fork identity, lineage docs, and WorkManager roadmap
  (`README.md`, `ROADMAP.md`, `AGENTS.md`)
- Temporary debug-build bridge: Firebase JobDispatcher via JitPack
  (`googlearchive/firebase-jobdispatcher-android`) until WorkManager migration

### Changed

- Port henrichg Android 7+ backup trigger/receiver fixes:
  - `com.zegoggles.smssync.BACKUP` broadcast registration
  - Manifest `android:exported` on receivers/components as required
  - Register `SmsBroadcastReceiver` / MMS filters from `App.onCreate()`
  - Incoming-scheduler related `App` / `MainActivity` wiring
- `minSdk` 24 (Android 7+)
- `compileSdk` 36 and `targetSdk` 35
- Android Gradle Plugin 8.9.1 and Gradle 8.11.1 (JDK 17)
- Android 12–15 build hardening for the new target:
  - Activity/component `android:exported` declarations
  - Foreground service `dataSync` type + `FOREGROUND_SERVICE_DATA_SYNC`
  - `POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM` permissions
  - `PendingIntent` `FLAG_IMMUTABLE`; `Context.RECEIVER_EXPORTED` on API 33+

### Notes

Still outstanding: WorkManager migration (Phase 2); fuller Android 14/15
runtime hardening and device testing (Phase 3); `targetSdk` 36 when appropriate.

## Older history

Release notes for the upstream SMS Backup+ lineage (through `1.5.11` and
unreleased `1.6.0` notes in-tree) are preserved in [`CHANGES`](CHANGES).
That file is an archive and is no longer updated.
