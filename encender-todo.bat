@echo off
start "roles-service" cmd /k ""%~dp0iniciar-roles.bat""
start "usuarios-service" cmd /k ""%~dp0iniciar-usuarios.bat""
start "compromisos-service" cmd /k ""%~dp0iniciar-compromisos.bat""
