# Changelog

All notable changes to **this unofficial experimental fork**
([shomanjk/sms-backup-plus](https://github.com/shomanjk/sms-backup-plus))
are documented in this file.

This project uses its own SemVer (`0.x` while experimental). Version numbers
here are **not** jberkel SMS Backup+ releases and are independent of henrichg
`1.7.0`.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [0.2.1] - 2026-07-20

`versionCode` 1808.

### Fixed

- Do not `cancelAll()` WorkManager jobs on every process start. Cold-starting for
  a ContentUriTrigger previously cancelled the observer that woke the app
  (dropping the SMS/MMS change) and could leave the 2‑hour REGULAR safety net
  cancelled, so backups stalled until the UI was opened. Process start now
  *ensures* jobs with `ExistingWorkPolicy.KEEP`; full cancel/reschedule remains
  for auto-backup settings changes. After each backup, re-ensure jobs are armed.

## [0.2.0] - 2026-07-17

`versionCode` 1807.

### Changed

- Replaced Firebase JobDispatcher / AlarmManagerDriver with AndroidX WorkManager
  (`SmsBackupWorker`, rewritten `BackupJobs`). Regular backups use unique
  one-time work rescheduled after each run; incoming uses ContentUriTrigger
  (primary) plus SMS/MMS broadcast receivers (secondary). ContentUriTrigger
  watches `content://sms` and `content://mms` (RCS often writes to MMS).
- Removed the “old scheduler” preference; WorkManager is the only scheduler.
- Request `POST_NOTIFICATIONS` at launch on Android 13+ (optional for backup).
- Calendar list uses `CALENDAR_DISPLAY_NAME` (Android 15+).
- Unit tests: Robolectric 4.14.1 + WorkManager test helpers.

### Fixed

- WorkManager backups now call `SmsBackupService.onCreate()` /
  `onDestroy()`, so the on-device sync log (`sms_backup_plus.log`) is written
  again and regular backups re-schedule after each run.
- Stop Otto `Object already registered` when leaving preference screens:
  `MainSettings` unregisters in `onStop` (not `onDestroy`).
- Flush sync-log writes so entries survive if the process is killed mid-backup.
- Drop WorkManager/JobScheduler network constraints for backup + ContentUriTrigger
  jobs (Samsung often leaves CONNECTIVITY unsatisfied in the background / batches
  network jobs). Enforce wifi/connectivity in-process in `SmsBackupService` instead.
- Incoming auto-backup no longer cancels itself when another SMS/MMS arrives
  mid-run (`ExistingWorkPolicy.KEEP` for INCOMING). Also post WorkManager
  `onStopped` cancel events on the main thread (Otto bus).

### Notes

- Physical-device RCS / Doze / OEM battery verification still required before
  calling auto-backup reliable for daily use.

## [0.1.4] - 2026-07-15

`versionCode` 1806.

### Fixed

- Calendar picker under Call log settings stays disabled when no calendars are
  loaded after enabling sync; request read+write calendar permission, refresh the
  list after grant, and show a clear “no calendars found” summary.
- Prompt for `READ_SMS` / `RECEIVE_SMS` / `RECEIVE_MMS` when automatic backup is
  enabled so incoming-message triggers can schedule backups (read-only SMS grant
  is not enough). Show an in-app explanation before the system dialog, avoid a
  bogus green “Permission problem” when a duplicate permission request is
  cancelled by Android, and after Deny (when Android will not re-prompt) offer
  Open settings instead of a silent no-op.

## [0.1.3] - 2026-07-14

`versionCode` 1805.

### Fixed

- IMAP TLS SNI on Android 12+ / high `targetSdk`: stop using K9’s blocked
  Conscrypt `SSLSocket#setHostname` reflection. Apply SNI via public
  `SSLParameters.setServerNames` (`SniAwareTrustedSocketFactory`).
- IMAP connect on dual-stack hosts (e.g. Gmail): try IPv4 before IPv6 so broken
  IPv6 paths do not burn K-9’s per-address timeouts (~5s each) before a working
  IPv4 connect (`Ipv4PreferringImapStore`).
- Edge-to-edge on `targetSdk` 35+: pad main UI for system bars so the toolbar
  and Backup/Restore controls are not under the status / navigation bars.

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

Historical entry for the fork’s initial cut. Later 0.2.0 WorkManager work
supersedes the JobDispatcher bridge. Remaining: physical RCS/OEM verify
(Phase 2) and fuller Android 14/15 device testing (Phase 3).


## Older history

Release notes for the upstream SMS Backup+ lineage (through `1.5.11` and
unreleased `1.6.0` notes in-tree) are preserved in [`CHANGES`](CHANGES).
That file is an archive and is no longer updated.
