#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
数据库定时备份脚本。
用法：
  python db_backup.py                  # 立即执行一次备份
  python db_backup.py --schedule       # 每天凌晨 2:30 自动执行
  python db_backup.py --clean-only     # 只清理过期备份

依赖环境变量（与 application.yml 保持一致）：
  GOV_DB_HOST       数据库主机，默认 127.0.0.1
  GOV_DB_PORT       数据库端口，默认 13306
  GOV_DB_USERNAME   数据库用户，默认 db_user
  GOV_DB_PASSWORD   数据库密码，默认 Egov@123
  GOV_DB_NAME       数据库名，默认 gov_db
  GOV_BACKUP_DIR    备份目录，默认 ./backups
  GOV_BACKUP_KEEP_DAYS  保留天数，默认 7
"""

import os
import sys
import subprocess
import datetime
import glob
import argparse
import time
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
log = logging.getLogger(__name__)

DB_HOST = os.environ.get("GOV_DB_HOST", "127.0.0.1")
DB_PORT = os.environ.get("GOV_DB_PORT", "13306")
DB_USER = os.environ.get("GOV_DB_USERNAME", "db_user")
DB_PASS = os.environ.get("GOV_DB_PASSWORD", "Egov@123")
DB_NAME = os.environ.get("GOV_DB_NAME", "gov_db")
BACKUP_DIR = os.environ.get("GOV_BACKUP_DIR", "./backups")
KEEP_DAYS = int(os.environ.get("GOV_BACKUP_KEEP_DAYS", "7"))


def ensure_backup_dir():
    os.makedirs(BACKUP_DIR, exist_ok=True)


def do_backup():
    ensure_backup_dir()
    ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = os.path.join(BACKUP_DIR, f"gov_db_{ts}.sql.gz")

    log.info("开始备份: %s -> %s", DB_NAME, filename)

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
            dump_proc = subprocess.Popen(dump_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env)
            gzip_proc = subprocess.Popen(["gzip", "-c"], stdin=dump_proc.stdout, stdout=out_file, stderr=subprocess.PIPE)
            dump_proc.stdout.close()
            gzip_out, gzip_err = gzip_proc.communicate()
            dump_proc.wait()

            if dump_proc.returncode != 0:
                _, dump_err = dump_proc.communicate()
                log.error("mysqldump 失败 returncode=%d err=%s", dump_proc.returncode, dump_err.decode("utf-8", errors="replace"))
                if os.path.exists(filename):
                    os.remove(filename)
                return False

            if gzip_proc.returncode != 0:
                log.error("gzip 压缩失败 returncode=%d err=%s", gzip_proc.returncode, gzip_err.decode("utf-8", errors="replace"))
                if os.path.exists(filename):
                    os.remove(filename)
                return False

        size_kb = os.path.getsize(filename) // 1024
        log.info("备份成功: %s (%d KB)", filename, size_kb)
        return True

    except FileNotFoundError as e:
        log.error("命令不存在，请确认 mysqldump/gzip 已安装: %s", e)
        return False
    except Exception as e:
        log.error("备份异常: %s", e)
        if os.path.exists(filename):
            os.remove(filename)
        return False


def clean_old_backups():
    ensure_backup_dir()
    pattern = os.path.join(BACKUP_DIR, "gov_db_*.sql.gz")
    files = glob.glob(pattern)
    cutoff = datetime.datetime.now() - datetime.timedelta(days=KEEP_DAYS)
    removed = 0
    for f in files:
        mtime = datetime.datetime.fromtimestamp(os.path.getmtime(f))
        if mtime < cutoff:
            os.remove(f)
            log.info("清理过期备份: %s (修改时间 %s)", f, mtime.strftime("%Y-%m-%d %H:%M:%S"))
            removed += 1
    if removed == 0:
        log.info("无过期备份需要清理（保留 %d 天）", KEEP_DAYS)
    else:
        log.info("共清理 %d 个过期备份", removed)


def run_once():
    ok = do_backup()
    clean_old_backups()
    return ok


def run_scheduled():
    log.info("定时备份已启动，每天 02:30 执行，保留 %d 天", KEEP_DAYS)
    while True:
        now = datetime.datetime.now()
        next_run = now.replace(hour=2, minute=30, second=0, microsecond=0)
        if next_run <= now:
            next_run += datetime.timedelta(days=1)
        wait_sec = (next_run - now).total_seconds()
        log.info("下次备份时间: %s（%d 秒后）", next_run.strftime("%Y-%m-%d %H:%M:%S"), int(wait_sec))
        time.sleep(wait_sec)
        run_once()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="gov_db 数据库备份工具")
    parser.add_argument("--schedule", action="store_true", help="定时模式，每天 02:30 自动执行")
    parser.add_argument("--clean-only", action="store_true", help="只清理过期备份，不执行备份")
    args = parser.parse_args()

    if args.clean_only:
        clean_old_backups()
    elif args.schedule:
        run_scheduled()
    else:
        success = run_once()
        sys.exit(0 if success else 1)
