# Local secrets (do not commit real values)

Ignored by `.gitignore` / `.cursorignore`:

- `keystore.properties`, `*.keystore`, `*.jks`
- `local.properties`
- `.env`, `.env.*`, `secrets/`, `PRIVATE/`
- `*.apk`, `*.aab`
- `notes.local.md`, `*.local.md`
- `.cursor/mcp.json`

Optional local-only dirs/files you can create (never commit):

- `PRIVATE/` — device notes, IMAP hostnames, sync-log excerpts
- `notes.local.md` — scratch pad
- `keystore.properties` — release signing (see `app/build.gradle`)
