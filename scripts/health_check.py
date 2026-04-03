#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
服务健康巡检脚本。
用法：
  python health_check.py               # 检查一次
  python health_check.py --watch 30    # 每 30 秒持续巡检
  python health_check.py --restart     # 检查失败时自动重启后端服务

依赖环境变量：
  GOV_HEALTH_URL      健康检查地址，默认 http://127.0.0.1:8080/api/health/ready
  GOV_SERVICE_NAME    systemd 服务名，默认 gov-backend（--restart 模式使用）
  GOV_ALERT_WEBHOOK   告警 Webhook URL（可选，POST JSON）
"""

import os
import sys
import time
import json
import argparse
import logging
import urllib.request
import urllib.error
import subprocess
import datetime

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
log = logging.getLogger(__name__)

HEALTH_URL = os.environ.get("GOV_HEALTH_URL", "http://127.0.0.1:8080/api/health/ready")
SERVICE_NAME = os.environ.get("GOV_SERVICE_NAME", "gov-backend")
ALERT_WEBHOOK = os.environ.get("GOV_ALERT_WEBHOOK", "")
TIMEOUT = 10


def check_once():
    """
    执行一次健康检查。
    返回 (ok: bool, detail: str)
    """
    try:
        req = urllib.request.Request(HEALTH_URL, headers={"User-Agent": "gov-health-check/1.0"})
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            body = resp.read().decode("utf-8")
            code = resp.getcode()
            if code == 200:
                log.info("健康检查通过 status=%d url=%s", code, HEALTH_URL)
                return True, body
            else:
                log.warning("健康检查异常 status=%d url=%s body=%s", code, HEALTH_URL, body[:200])
                return False, f"HTTP {code}: {body[:200]}"
    except urllib.error.HTTPError as e:
        detail = f"HTTP {e.code}: {e.reason}"
        log.error("健康检查失败 %s url=%s", detail, HEALTH_URL)
        return False, detail
    except urllib.error.URLError as e:
        detail = f"连接失败: {e.reason}"
        log.error("健康检查失败 %s url=%s", detail, HEALTH_URL)
        return False, detail
    except Exception as e:
        detail = f"异常: {e}"
        log.error("健康检查异常 %s url=%s", detail, HEALTH_URL)
        return False, detail


def send_alert(message):
    """发送告警到 Webhook（可选）。"""
    if not ALERT_WEBHOOK:
        return
    try:
        payload = json.dumps({
            "text": message,
            "timestamp": datetime.datetime.now().isoformat()
        }).encode("utf-8")
        req = urllib.request.Request(
            ALERT_WEBHOOK,
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=5):
            pass
        log.info("告警已发送到 Webhook")
    except Exception as e:
        log.warning("告警发送失败: %s", e)


def restart_service():
    """尝试通过 systemctl 重启服务。"""
    log.warning("尝试重启服务: %s", SERVICE_NAME)
    try:
        result = subprocess.run(
            ["systemctl", "restart", SERVICE_NAME],
            capture_output=True, text=True, timeout=60
        )
        if result.returncode == 0:
            log.info("服务重启成功: %s", SERVICE_NAME)
            return True
        else:
            log.error("服务重启失败: %s stderr=%s", SERVICE_NAME, result.stderr)
            return False
    except FileNotFoundError:
        log.error("systemctl 不可用，请手动重启服务")
        return False
    except Exception as e:
        log.error("重启服务异常: %s", e)
        return False


def watch(interval_sec, auto_restart):
    """持续巡检模式。"""
    fail_count = 0
    max_fail_before_restart = 3

    log.info("持续巡检已启动 interval=%ds url=%s restart=%s", interval_sec, HEALTH_URL, auto_restart)
    while True:
        ok, detail = check_once()
        if ok:
            fail_count = 0
        else:
            fail_count += 1
            msg = f"[gov-backend] 健康检查失败 ({fail_count}次) url={HEALTH_URL} detail={detail}"
            send_alert(msg)

            if auto_restart and fail_count >= max_fail_before_restart:
                log.error("连续失败 %d 次，触发自动重启", fail_count)
                send_alert(f"[gov-backend] 连续失败 {fail_count} 次，正在自动重启 {SERVICE_NAME}")
                restarted = restart_service()
                if restarted:
                    fail_count = 0
                    # 等待服务启动
                    time.sleep(30)

        time.sleep(interval_sec)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="gov-backend 服务健康巡检")
    parser.add_argument("--watch", type=int, metavar="SECONDS", help="持续巡检间隔秒数")
    parser.add_argument("--restart", action="store_true", help="连续失败 3 次后自动重启服务")
    args = parser.parse_args()

    if args.watch:
        watch(args.watch, args.restart)
    else:
        ok, detail = check_once()
        sys.exit(0 if ok else 1)
