@echo OFF

SET CurrentDirectory=%~dp0
SET PowerShellScript=%CurrentDirectory%support-diagnostics.ps1

:loop
IF "%~1"=="" GOTO cont
IF /I "%~1"=="-H" SET H="%~2"
IF /I "%~1"=="-o" SET o="%~2"
IF /I "%~1"=="-n" SET n="%~2"
IF /I "%~1"=="-nc" SET nc=1
IF /I "%~1"=="-r" SET r="%~2"
IF /I "%~1"=="-i" SET i="%~2"
IF /I "%~1"=="-a" SET a="%~2"
IF /I "%~1"=="-c" SET c="%~2"
IF /I "%~1"=="-p" SET p="%~2"
IF /I "%~1"=="-t" SET t="%~2"
SHIFT & GOTO loop
:cont

SET Command="& '%PowerShellScript%' -H '%H%' -o '%o%' -n '%n%' -r '%r%' -i '%i%' -a '%a%' -c '%c%' -p '%p%' -t '%t%'"
IF "%nc%" == "1" (
	SET Command=%Command% -nc
)

powershell -NoProfile -ExecutionPolicy Bypass -Command %Command%

SET H=
SET o=
SET n=
SET nc=
SET r=
SET i=
SET a=
SET c=
SET p=
