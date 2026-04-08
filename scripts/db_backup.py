#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""MariaDB backup helper for the gov backend."""

import argparse
import datetime
import glob
import logging
import os
import subprocess
import sys
import time


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
log = logging.getLogger(__name__)

DB_HOST = os.environ.get("GOV_DB_HOST", "127.0.0.1")
DB_PORT = os.environ.get("GOV_DB_PORT", "13306")
DB_USER = os.environ.get("GOV_DB_USERNAME", "db_user")
DB_PASS = os.environ.get("GOV_DB_PASSWORD", "")
DB_NAME = os.environ.get("GOV_DB_NAME", "gov_db")
BACKUP_DIR = os.environ.get("GOV_BACKUP_DIR", "./backups")
KEEP_DAYS = int(os.environ.get("GOV_BACKUP_KEEP_DAYS", "7"))


def ensure_backup_dir():
    os.makedirs(BACKUP_DIR, exist_ok=True)


def validate_required_config():
    if DB_PASS and DB_PASS != "CHANGE_ME":
        return True
    log.error("Missing GOV_DB_PASSWORD. Set a real password before running backups.")
    return False


def do_backup():
    if not validate_required_config():
        return False

    ensure_backup_dir()
    ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = os.path.join(BACKUP_DIR, f"gov_db_{ts}.sql.gz")

    log.info("Starting backup: %s -> %s", DB_NAME, filename)

    env = os.environ.copy()
    env["MYSQL_PWD"] = DB_PASS

    dump_cmd = [
        "mysqldump",
        f"--host={DB_HOST}",
        f"--port={DB_PORT}",
        f"--user={DB_USER}",
        "--single-transaction",
        "--routines",
        "--triggers",
        "--set-gtid-purged=OFF",
        DB_NAME,
    ]

    try:
        with open(filename, "wb") as out_file:
            dump_proc = subprocess.Popen(
                dump_cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                env=env,
            )
            gzip_proc = subprocess.Popen(
                ["gzip", "-c"],
                stdin=dump_proc.stdout,
                stdout=out_file,
                stderr=subprocess.PIPE,
            )
            dump_proc.stdout.close()
            _, gzip_err = gzip_proc.communicate()
            _, dump_err = dump_proc.communicate()

            if dump_proc.returncode != 0:
                log.error(
                    "mysqldump failed returncode=%d err=%s",
                    dump_proc.returncode,
                    dump_err.decode("utf-8", errors="replace"),
                )
                if os.path.exists(filename):
                    os.remove(filename)
                return False

            if gzip_proc.returncode != 0:
                log.error(
                    "gzip failed returncode=%d err=%s",
                    gzip_proc.returncode,
                    gzip_err.decode("utf-8", errors="replace"),
                )
                if os.path.exists(filename):
                    os.remove(filename)
                return False

        size_kb = os.path.getsize(filename) // 1024
        log.info("Backup completed: %s (%d KB)", filename, size_kb)
        return True
    except FileNotFoundError as exc:
        log.error("Command not found. Ensure mysqldump and gzip are installed: %s", exc)
        return False
    except Exception as exc:  # noqa: BLE001
        log.error("Backup failed: %s", exc)
        if os.path.exists(filename):
            os.remove(filename)
        return False


def clean_old_backups():
    ensure_backup_dir()
    pattern = os.path.join(BACKUP_DIR, "gov_db_*.sql.gz")
    files = glob.glob(pattern)
    cutoff = datetime.datetime.now() - datetime.timedelta(days=KEEP_DAYS)
    removed = 0

    for backup_file in files:
        mtime = datetime.datetime.fromtimestamp(os.path.getmtime(backup_file))
        if mtime < cutoff:
            os.remove(backup_file)
            log.info(
                "Removed expired backup: %s (mtime %s)",
                backup_file,
                mtime.strftime("%Y-%m-%d %H:%M:%S"),
            )
            removed += 1

    if removed == 0:
        log.info("No expired backups to remove (keep %d days)", KEEP_DAYS)
    else:
        log.info("Removed %d expired backups", removed)


def run_once():
    ok = do_backup()
    clean_old_backups()
    return ok


def run_scheduled():
    if not validate_required_config():
        return False

    log.info("Backup scheduler started, daily at 02:30, keep %d days", KEEP_DAYS)
    while True:
        now = datetime.datetime.now()
        next_run = now.replace(hour=2, minute=30, second=0, microsecond=0)
        if next_run <= now:
            next_run += datetime.timedelta(days=1)
        wait_seconds = (next_run - now).total_seconds()
        log.info(
            "Next backup at %s, sleeping %d seconds",
            next_run.strftime("%Y-%m-%d %H:%M:%S"),
            int(wait_seconds),
        )
        time.sleep(wait_seconds)
        run_once()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="gov_db backup helper")
    parser.add_argument("--schedule", action="store_true", help="Run daily at 02:30")
    parser.add_argument("--clean-only", action="store_true", help="Only remove expired backups")
    args = parser.parse_args()

    if args.clean_only:
        clean_old_backups()
    elif args.schedule:
        sys.exit(0 if run_scheduled() is not False else 1)
    else:
        sys.exit(0 if run_once() else 1)
