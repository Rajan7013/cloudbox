@echo off
title CloudBox Local Website Server
cd /d "%~dp0"
if exist "website\serve_website.py" (
    python website\serve_website.py
) else if exist "serve_website.py" (
    python serve_website.py
) else (
    python -m http.server 8000
)
pause
