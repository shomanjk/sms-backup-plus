# Roadmap

This experimental fork focuses first on making automatic backup dependable on current Android versions. The phases are ordered by dependency and risk; this is not yet a daily-use release plan.

## Phase 1: Port scheduler and receiver fixes

- Port and verify henrichg's Android 7+ backup trigger and receiver fixes on top of the jberkel `master` lineage.
- Preserve the upstream RCS and group-message fixes.
- Add focused regression coverage for manual, incoming, periodic, and reboot-triggered backup paths.

## Phase 2: Migrate to WorkManager

**Implemented on branch `phase-2-workmanager`** (logic ported from Mibou’s WorkManager work, without package rename). JobDispatcher / `SmsJobService` / `AlarmManagerDriver` are gone; `SmsBackupWorker` + rewritten `BackupJobs` schedule unique one-time work, ContentUriTrigger incoming (with SMS/MMS broadcast as secondary), network constraints, and reschedule after REGULAR completion.

Completion criteria:

- [x] No Firebase JobDispatcher dependency, manifest service, or compatibility driver remains.
- [x] Unit suite green (`BackupJobsTest`, `SmsBackupWorkerTest`, full `./gradlew test`).
- [x] Emulator smoke: install/launch, WorkManager SystemJobService + ContentUri jobs present.
- [ ] Physical-device confirmation: RCS/non-`SMS_RECEIVED` incoming, reboot, Doze / OEM battery limits.

Do **not** claim auto-backup is fixed for daily use until the physical-device checks pass.

## Phase 3: Harden for Android 14 and 15

`compileSdk` is **36** and `targetSdk` is **35**. Remaining work is runtime behavior and OEM testing, not raising those numbers again (except a later bump to **target 36** if/when Play or device testing warrants it).

- [x] `POST_NOTIFICATIONS` runtime request (optional; non-blocking if denied).
- [x] FGS `dataSync` + typed FGS permission (already present).
- [x] Calendar `CALENDAR_DISPLAY_NAME` for Android 15+ list population.
- Audit remaining exact-alarm UX (permission currently unused after WorkManager).
- Validate restricted-settings / battery optimization on clean installs and upgrades.
- Test battery optimization, Doze, reboot, connectivity changes, and long-running backups on Android 14 and 15.
- Document known OEM limitations and any user action needed for reliable scheduling.

## Release readiness

After Phase 2 device verification and Phase 3 hardening, run upgrade and fresh-install tests, verify that IMAP backup state does not cause duplicate uploads, and publish clearly labeled experimental artifacts before considering a daily-use release.
