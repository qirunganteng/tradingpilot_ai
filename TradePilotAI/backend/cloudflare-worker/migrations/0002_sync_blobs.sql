-- PRD §9.2 covers a fully-normalized per-column schema (bookmarks,
-- bookmark_folders, history, workspaces, watchlist, journal_entries,
-- price_alerts, each with its own explicit column set) assuming full user
-- accounts (users table with password_hash, JWT login -- PRD §10.1). That
-- login/account layer doesn't exist in this backend yet (it's device-token
-- scoped, same as `analyses`/`request_log` in 0001_init.sql), so a
-- per-column schema tied to a `user_id` that doesn't exist anywhere would
-- be unusable today and force either fabricating a fake user system or
-- leaving the tables permanently empty.
--
-- Instead: one generic, device-scoped `sync_blobs` table. Each row is
-- "the current state of one syncable data type, for one device", stored
-- as the exact JSON each Flutter-side manager already produces
-- (WorkspaceManager, HistoryManager, PasswordVault, PermissionManager,
-- DownloadManager -- see app/lib/features/browser_core/services/, and
-- docs/known-limitations.md's Sync entry, which this migration is the
-- first real step toward closing). This intentionally does NOT yet give
-- multi-device sync (two devices with different device_ids never see each
-- other's data) -- it gives device-scoped cloud backup/restore today
-- (survives an uninstall/reinstall or a new PC), and becomes real
-- cross-device sync later purely by adding a `user_id` column and an
-- account/login layer on top, without changing this table's shape.
CREATE TABLE IF NOT EXISTS sync_blobs (
    device_id TEXT NOT NULL,
    data_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (device_id, data_type)
);

CREATE INDEX IF NOT EXISTS idx_sync_blobs_device
    ON sync_blobs (device_id);
