REM BEFORE RUNNING SET THE %JAVA19_HOME% AND %MVN3_HOME% TO THE BIN FOLDERS OF THE PROGRAMS

@echo off

call build
call run
call checkState

:build
if %MVN3_HOME%=="" (mvn clean package) else (start %MVN3_HOME%/mvn clean package)
exit /b 0

:run
if %JAVA19_HOME%=="" (start "hamleyproc" java -jar hamley.jar) else (start "hamleyproc" %JAVA19_HOME%/java -jar hamley.jar)
exit /b 0

:checkState
tasklist /V /FI "WindowTitle eq hamleyproc*">temp.txt
set /p var1789= < temp.txt
if %var1789%=="" call run
goto checkState