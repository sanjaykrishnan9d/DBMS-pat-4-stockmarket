@echo off
setlocal enabledelayedexpansion

echo ============================================================================
echo   STOCK MARKET MANAGEMENT SYSTEM - BUILD SCRIPT
echo ============================================================================

:: 1. Detect or configure JAVA_HOME
if "%JAVA_HOME%"=="" (
    if exist "C:\Program Files\Java\jdk-27" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-27"
    ) else if exist "C:\Program Files\Java\latest" (
        set "JAVA_HOME=C:\Program Files\Java\latest"
    )
)

if "%JAVA_HOME%"=="" (
    echo [ERROR] JAVA_HOME is not set and no default JDK was found!
    echo Please set JAVA_HOME to your JDK directory, e.g.:
    echo set JAVA_HOME=C:\Program Files\Java\jdk-27
    pause
    exit /b 1
)

echo [INFO] Using JDK: %JAVA_HOME%
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: 2. Detect Tomcat lib for servlet-api.jar
set "SERVLET_JAR="
if exist "lib\servlet-api.jar" (
    set "SERVLET_JAR=lib\servlet-api.jar"
) else if exist "WebContent\WEB-INF\lib\servlet-api.jar" (
    set "SERVLET_JAR=WebContent\WEB-INF\lib\servlet-api.jar"
) else if exist "C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\lib\servlet-api.jar" (
    set "SERVLET_JAR=C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\lib\servlet-api.jar"
    copy "C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\lib\servlet-api.jar" "lib\" >nul 2>&1
)

if "%SERVLET_JAR%"=="" (
    echo [ERROR] servlet-api.jar not found! Please place it in lib\ or check Tomcat path.
    pause
    exit /b 1
)

:: 3. Detect Oracle JDBC Driver
set "OJDBC_JAR="
if exist "lib\ojdbc6.jar" (
    set "OJDBC_JAR=lib\ojdbc6.jar"
) else if exist "lib\ojdbc8.jar" (
    set "OJDBC_JAR=lib\ojdbc8.jar"
) else if exist "lib\ojdbc11.jar" (
    set "OJDBC_JAR=lib\ojdbc11.jar"
) else if exist "C:\oraclexe\app\oracle\product\11.2.0\server\jdbc\lib\ojdbc6.jar" (
    set "OJDBC_JAR=C:\oraclexe\app\oracle\product\11.2.0\server\jdbc\lib\ojdbc6.jar"
    copy "%OJDBC_JAR%" "WebContent\WEB-INF\lib\" >nul 2>&1
    copy "%OJDBC_JAR%" "lib\" >nul 2>&1
)

echo [INFO] Found Servlet API: %SERVLET_JAR%
echo [INFO] Found Oracle Driver: %OJDBC_JAR%

:: 4. Prepare compilation output folders
if not exist "WebContent\WEB-INF\classes" mkdir "WebContent\WEB-INF\classes"
if not exist "WebContent\WEB-INF\lib" mkdir "WebContent\WEB-INF\lib"

:: Copy db.properties to classes
if exist "src\main\resources\db.properties" (
    copy /Y "src\main\resources\db.properties" "WebContent\WEB-INF\classes\" >nul
)

:: 5. Compile all Java source files
echo [INFO] Compiling Java classes...
dir /s /b src\main\java\*.java > sources.tmp
"%JAVA_HOME%\bin\javac" -encoding UTF-8 -cp "%SERVLET_JAR%;%OJDBC_JAR%;WebContent\WEB-INF\lib\*" -d "WebContent\WEB-INF\classes" @sources.tmp
if errorlevel 1 (
    echo [ERROR] Java compilation failed!
    del sources.tmp
    pause
    exit /b 1
)
del sources.tmp
echo [SUCCESS] Java compilation completed.

:: 6. Package into WAR file
echo [INFO] Packaging StockMarketManagement.war...
cd WebContent
"%JAVA_HOME%\bin\jar" -cvf "..\StockMarketManagement.war" * >nul
cd ..
echo [SUCCESS] StockMarketManagement.war generated successfully!

:: 7. Check if Tomcat webapps folder is accessible to auto-deploy
set "TOMCAT_WEBAPPS="
if exist "C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\webapps" (
    set "TOMCAT_WEBAPPS=C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\webapps"
)

if not "%TOMCAT_WEBAPPS%"=="" (
    echo [INFO] Deploying to Tomcat at: %TOMCAT_WEBAPPS%
    copy /Y "StockMarketManagement.war" "%TOMCAT_WEBAPPS%\" >nul
    echo [SUCCESS] Deployed StockMarketManagement.war to Tomcat webapps!
)

echo ============================================================================
echo   BUILD AND DEPLOYMENT COMPLETE!
echo ============================================================================
echo Run Tomcat:
echo   C:\apache-tomcat-9.0.123-windows-x64\apache-tomcat-9.0.123\bin\startup.bat
echo Access application at:
echo   http://localhost:8080/StockMarketManagement/
echo ============================================================================
