@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "D:\tradePilot Ai claude\TradePilotAI\desktop-client"
call gradlew.bat :app:compileKotlin --console=plain > "D:\tradePilot Ai claude\_build_log2.txt" 2>&1
echo DONE > "D:\tradePilot Ai claude\_build_done2.txt"
