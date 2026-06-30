@echo off
:: Launches CineBook from the correct working directory
cd /d "C:\Users\Sruth\OneDrive\Desktop\Movie ticket booking"
start "" "C:\Users\Sruth\.vscode\extensions\redhat.java-1.55.0-win32-x64\jre\21.0.11-win32-x86_64\bin\javaw.exe" -cp ".;lib/sqlite-jdbc-3.34.0.jar;lib/flatlaf-3.4.1.jar" MovieBookingSystem
