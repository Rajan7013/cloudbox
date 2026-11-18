"""
Simple local development server for CloudBox website with correct APK MIME types and headers.
Run this script to test downloading CloudBox-v1.0.apk on your computer or mobile phone.
"""

import http.server
import mimetypes
import os
import socket
import sys

PORT = 8000
DIRECTORY = os.path.dirname(os.path.abspath(__file__))

# Ensure APK MIME type is recognized
mimetypes.add_type("application/vnd.android.package-archive", ".apk")


class CloudBoxHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Force download headers for .apk files
        if self.path.endswith(".apk"):
            self.send_header("Content-Type", "application/vnd.android.package-archive")
            self.send_header("Content-Disposition", f'attachment; filename="{os.path.basename(self.path)}"')
            self.send_header("Cache-Control", "no-cache")
        super().end_headers()


def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


if __name__ == "__main__":
    local_ip = get_local_ip()
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), CloudBoxHTTPRequestHandler)
    print("=" * 60)
    print(" CloudBox Website & APK Download Server Running!")
    print("=" * 60)
    print(f"  * On this PC:         http://localhost:{PORT}")
    print(f"  * On your Phone:      http://{local_ip}:{PORT}")
    print(f"  * Direct APK Test:    http://localhost:{PORT}/downloads/CloudBox-v1.0.apk")
    print("=" * 60)
    print("Press Ctrl+C to stop the server.\n")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServer stopped.")
        sys.exit(0)
