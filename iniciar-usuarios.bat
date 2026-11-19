@echo off
set "JBR=%ProgramFiles%\Android\Android Studio\jbr"
if not exist "%JBR%\bin\java.exe" set "JBR=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if exist "%JBR%\bin\java.exe" set "JAVA_HOME=%JBR%"
if not defined JAVA_HOME (
    echo No se encontro Java. Instala Android Studio o define JAVA_HOME.
    pause
    exit /b 1
)
echo Iniciando usuarios-service en http://localhost:8082 ... (para detenerlo: Ctrl+C)
cd /d "%~dp0usuarios-service"
call .\mvnw.cmd spring-boot:run
if errorlevel 1 pause
