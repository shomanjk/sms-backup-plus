# Roadmap

This experimental fork focuses first on making automatic backup dependable on current Android versions. The phases are ordered by dependency and risk; this is not yet a daily-use release plan.

## Phase 1: Port scheduler and receiver fixes

- Port and verify henrichg's Android 7+ backup trigger and receiver fixes on top of the jberkel `master` lineage.
- Preserve the upstream RCS and group-message fixes.
- Add focused regression coverage for manual, incoming, periodic, and reboot-triggered backup paths.

## Phase 2: Migrate to WorkManager

This is the main automatic-backup fix. Replace Firebase JobDispatcher and its custom scheduling paths with AndroidX WorkManager, including periodic work, immediate/incoming-message work, network constraints, retries, persistence across reboot, and cancellation/rescheduling when preferences change.

The migration is necessary because Firebase JobDispatcher is deprecated and unsupported. The [henrichg maintainer explicitly noted that the dependency is no longer available and migration to WorkManager is required](https://github.com/henrichg/sms-backup-plus/issues/2). Android also provides an official [Firebase JobDispatcher to WorkManager migration guide](https://developer.android.com/develop/background-work/background-tasks/persistent/migrate-from-legacy/firebase).

Completion criteria:

- No Firebase JobDispatcher dependency, manifest service, or compatibility driver remains.
- Automatic backup survives process death and device reboot within Android background-execution limits.
- Existing schedule, network, retry, and user-cancellation semantics are documented and tested.

## Phase 3: Harden for Android 14 and 15

`compileSdk` is **36** and `targetSdk` is **35**. Remaining work is runtime behavior and OEM testing, not raising those numbers again (except a later bump to **target 36** if/when Play or device testing warrants it).

- Audit foreground-service, background-start, exact-alarm, broadcast-receiver, and notification requirements (POST_NOTIFICATIONS runtime prompt, exact-alarm UX, FGS types).
- Validate runtime permissions and restricted-setting behavior on clean installs and upgrades.
- Test battery optimization, Doze, reboot, connectivity changes, and long-running backups on Android 14 and 15.
- Document known OEM limitations and any user action needed for reliable scheduling.

## Release readiness

After all three phases, run upgrade and fresh-install tests, verify that IMAP backup state does not cause duplicate uploads, and publish clearly labeled experimental artifacts before considering a daily-use release.
