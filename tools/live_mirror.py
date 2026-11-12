#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
实时手机镜像：把 adb 设备画面以 MJPEG 流推给浏览器。

用途：调玻璃材质时不再等 Paparazzi 快照——手机插着 USB，
浏览器里看到的就是实时真机渲染（折射、模糊、辉光、滚动、按压全保真）。

用法：
    python tools/live_mirror.py
    然后打开 http://127.0.0.1:8631/

代码改动后热装：
    gradle installDebug && adb shell am start -n com.popsmoke.tracker/.MainActivity

依赖：Pillow（pip install pillow）。帧率约 2-5 fps（screencap 是全屏 PNG，够看材质）。
"""
import subprocess
import threading
import io
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

try:
    from PIL import Image, ImageDraw
except ImportError:
    raise SystemExit("需要 Pillow：pip install pillow")

ADB = r"I:/dev/toolchain/android-sdk/platform-tools/adb.exe"
PORT = 8631
SCALE = 0.6
QUALITY = 70

_state = {"jpeg": None, "device": False}
_lock = threading.Lock()


def has_device() -> bool:
    out = subprocess.run([ADB, "devices"], capture_output=True, text=True).stdout
    return any(
        "device" in line and not line.startswith("List") and "\tunauthorized" not in line
        for line in out.splitlines()[1:]
        if line.strip()
    )


def placeholder(text: str) -> bytes:
    img = Image.new("RGB", (360, 780), (18, 18, 24))
    d = ImageDraw.Draw(img)
    d.text((24, 366), text, fill=(190, 190, 205))
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=60)
    return buf.getvalue()


def capture_loop():
    while True:
        if not has_device():
            with _lock:
                _state["device"] = False
                _state["jpeg"] = placeholder(
                    "Waiting for device...\n\nplug in phone via USB\n(USB debugging enabled)"
                )
            time.sleep(1.0)
            continue
        try:
            t0 = time.time()
            p = subprocess.run(
                [ADB, "exec-out", "screencap", "-p"],
                capture_output=True, timeout=6,
            )
            img = Image.open(io.BytesIO(p.stdout)).convert("RGB")
            if SCALE != 1.0:
                img = img.resize(
                    (int(img.width * SCALE), int(img.height * SCALE))
                )
            buf = io.BytesIO()
            img.save(buf, "JPEG", quality=QUALITY)
            with _lock:
                _state["device"] = True
                _state["jpeg"] = buf.getvalue()
            # screencap 本身 ~0.2-0.4s，不再额外休眠，帧率自然落在 2-5fps
            elapsed = time.time() - t0
            if elapsed < 0.25:
                time.sleep(0.25 - elapsed)
        except Exception:
            time.sleep(0.5)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/stream":
            self.send_response(200)
            self.send_header(
                "Content-Type", "multipart/x-mixed-replace; boundary=frame"
            )
            self.end_headers()
            try:
                while True:
                    with _lock:
                        frame = _state["jpeg"]
                    if frame is None:
                        time.sleep(0.1)
                        continue
                    self.wfile.write(
                        b"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: "
                        + str(len(frame)).encode()
                        + b"\r\n\r\n" + frame + b"\r\n"
                    )
                    time.sleep(0.06)
            except Exception:
                pass  # 客户端断开，正常
        else:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(
                b'<html><head><title>PopSmoke Live</title></head>'
                b'<body style="margin:0;background:#141419;display:flex;'
                b'justify-content:center;align-items:center;height:100vh">'
                b'<img src="/stream" style="max-height:96vh;border-radius:12px">'
                b"</body></html>"
            )

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    threading.Thread(target=capture_loop, daemon=True).start()
    print("live mirror: http://127.0.0.1:%d" % PORT)
    ThreadingHTTPServer(("127.0.0.1", PORT), Handler).serve_forever()
