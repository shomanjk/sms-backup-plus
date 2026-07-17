# AGENTS.md — SMS Backup+ (maintained fork)

Guidance for AI agents and humans working in this repository.

## What this repo is

- **Public GitHub:** https://github.com/shomanjk/sms-backup-plus
- **Unofficial experimental fork** of [jberkel/sms-backup-plus](https://github.com/jberkel/sms-backup-plus). Not affiliated with jberkel or henrichg. **Not ready for daily use.**
- **Local clone path (this machine):** this directory is the Cursor project root (historically also referred to as `sms-backup-plus-maintained`).

## Lineage

| Remote | URL | Role |
|--------|-----|------|
| `origin` | `git@github.com:shomanjk/sms-backup-plus.git` | This fork |
| `upstream` | `https://github.com/jberkel/sms-backup-plus.git` | Canonical upstream (RCS / group-thread work on `master`) |
| `henrichg` | `https://github.com/henrichg/sms-backup-plus.git` | Source of Android 7+ receiver / broadcast / incoming-scheduler ports |

**Current app identity**

- `applicationId`: `com.zegoggles.smssync` (same as Play / henrichg — sideload requires uninstall; signatures differ)
- `versionName`: `0.2.0` / `versionCode`: `1807` (fork SemVer; independent of upstream)
- `minSdkVersion`: `24` (Android 7+)
- `compileSdk`: `36` / `targetSdkVersion`: `35`

## What was already done

1. Forked jberkel `master` (keeps RCS / group MMS threading fixes from [#1097](https://github.com/jberkel/sms-backup-plus/pull/1097)).
2. Ported henrichg functional fixes (not Android Studio bump noise):
   - `com.zegoggles.smssync.BACKUP` broadcast via runtime registration + `RECEIVER_EXPORTED` where needed
   - Manifest `android:exported` on relevant receivers
   - `SmsBroadcastReceiver` / MMS registration from `App.onCreate()`
   - Incoming scheduler / related App + MainActivity wiring
3. Documented experimental status in `README.md` and phases in `ROADMAP.md`.
4. **WorkManager migration** (branch `phase-2-workmanager`): `SmsBackupWorker` + rewritten `BackupJobs`; removed JobDispatcher / `SmsJobService` / `AlarmManagerDriver`. ContentUriTrigger primary for incoming; SMS/MMS broadcasts kept as secondary. Port informed by [Mibou/sms-backup-plus](https://github.com/Mibou/sms-backup-plus) `fix/calendar-display-name-android15` (no package rename).
5. Fork release notes live in `CHANGELOG.md` (Keep a Changelog); upstream history archived in `CHANGES`.

## Goal / roadmap

See [ROADMAP.md](ROADMAP.md). Priority order:

1. ~~Port henrichg trigger/receiver fixes~~ (largely done; still verify on device)
2. ~~Replace Firebase JobDispatcher with WorkManager~~ (code done on `phase-2-workmanager`; **physical RCS/OEM verify before claiming auto-backup fixed**)
3. Harden for Android 14/15 (remaining OEM / Doze / restricted-settings testing)

Do **not** claim auto-backup is fixed for daily use until physical-device verification in ROADMAP Phase 2 is checked off.

## Hot files

- `app/src/main/java/com/zegoggles/smssync/App.java` — receiver registration, WorkManager `Configuration.Provider`, reschedule
- `app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java` — WorkManager scheduling
- `app/src/main/java/com/zegoggles/smssync/service/SmsBackupWorker.java` — ListenableWorker entry
- `app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java`
- `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle` / root `build.gradle`
- Version / about UX: `Preferences.consumeVersionDialog()`, `Dialogs.VersionNotes`,
  `ui_dialog_welcome_*` / `ui_dialog_whats_new_*` in `strings.xml`,
  `app/src/main/assets/about.html` (menu About only)

## Release / user-facing copy checklist

When shipping a user-visible change (or a version bump), **proactively update or
suggest updating**:

1. `CHANGELOG.md`
2. `ui_dialog_whats_new_msg` (upgrade dialog; keep short)
3. `about.html` “New in this release” list (and fork warnings if status changed)
4. `ui_dialog_welcome_msg` only if install/disclaimer guidance changed
5. `versionName` / `versionCode` in `app/build.gradle` plus this file and
   `.cursor/rules/fork-context.mdc` when bumping

Do **not** claim auto-backup is fixed in those dialogs until physical-device Phase 2 checks pass.

## Agent constraints

- Do **not** commit secrets: keystores, `keystore.properties`, `local.properties`, `.env`, APKs, MCP tokens.
- Do **not** change `applicationId` without an explicit user decision (affects install/replace vs side-by-side).
- Prefer small, focused changes; preserve RCS/group threading behavior from jberkel.
- Skip merging open upstream PRs (e.g. Android 15 package-rename experiments) unless asked.
- When building: JDK 17; `./gradlew assembleDebug`.
- Install testing: uninstall other `com.zegoggles.smssync` builds first; after wipe, use **Skip** on first backup if IMAP already has history.

## Sibling clone (do not confuse)

`../sms-backup-plus` may still be a checkout of **henrichg** only. Work and Cursor project for this fork live **here**.
