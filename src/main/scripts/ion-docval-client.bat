@echo off
set SPATH=%~dp0
set LIB=%SPATH%\..\lib
set CLASSPATH=%LIB%\*;%LIB%\ion-docval-*.jar

java net.ionite.docval.server.DocValHttpClientMain %*