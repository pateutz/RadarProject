@echo off
for /f "tokens=14" %%a in ('ipconfig ^| findstr IPv4') do set _IPaddr=%%a
echo YOUR IP ADDRESS IS: %_IPaddr%
echo "RUNNING PUBG-RADAR"
set /p game=ENTER GAME PC IP:
echo "%game%"
java -jar target\VMRadar-1.2.1-jar-with-dependencies.jar %_IPaddr% PortFilter %game%
pause