@echo off
start "eureka-server" cmd /k ""%~dp0iniciar-eureka.bat""
echo Esperando 20 segundos a que Eureka arranque...
timeout /t 20 /nobreak >nul
start "roles-service" cmd /k ""%~dp0iniciar-roles.bat""
start "usuarios-service" cmd /k ""%~dp0iniciar-usuarios.bat""
start "compromisos-service" cmd /k ""%~dp0iniciar-compromisos.bat""
start "api-gateway" cmd /k ""%~dp0iniciar-gateway.bat""
