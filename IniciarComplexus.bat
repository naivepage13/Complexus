@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1
title Complexus - servidor local
cd /d "%~dp0"

echo.
echo  ==========================================================
echo    COMPLEXUS - iniciando o servidor local
echo  ==========================================================
echo.

REM ---------------------------------------------------------------------
REM  Ja esta no ar? Entao so abre o navegador e sai.
REM ---------------------------------------------------------------------
netstat -an | findstr /r /c:"TCP.*:8080.*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo  O servidor ja esta rodando. Abrindo o navegador...
    start "" "http://localhost:8080"
    echo.
    call :pausa
    exit /b 0
)

REM ---------------------------------------------------------------------
REM  Maven
REM ---------------------------------------------------------------------
where mvn >nul 2>&1
if errorlevel 1 (
    echo  [ERRO] Maven nao foi encontrado no PATH.
    echo.
    echo  Instale o Apache Maven 3.9 ou superior e garanta que a pasta
    echo  bin dele esteja no PATH do Windows.
    goto :erro
)

REM ---------------------------------------------------------------------
REM  JDK 21 ou superior
REM
REM  O projeto compila com release 21. O JAVA_HOME da maquina pode estar
REM  apontando para uma versao mais antiga, entao procuramos um JDK
REM  compativel nos lugares usuais e usamos ele apenas nesta janela.
REM ---------------------------------------------------------------------
set "JDK="
for /f "usebackq delims=" %%J in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$c=@(); if($env:JAVA_HOME){$c+=$env:JAVA_HOME}; $r=@((Join-Path $env:USERPROFILE '.jdks'),'C:\Ferramentas','C:\Program Files\Java','C:\Program Files\Eclipse Adoptium','C:\Program Files\Microsoft','C:\Program Files\Amazon Corretto','C:\Program Files\Zulu'); foreach($d in $r){ if(Test-Path $d){ $c+=(Get-ChildItem $d -Directory -ErrorAction SilentlyContinue).FullName } }; foreach($p in $c){ if(Test-Path (Join-Path $p 'bin\javac.exe')){ $t=(Get-Content (Join-Path $p 'release') -ErrorAction SilentlyContinue) -join ' '; if($t -match 'JAVA_VERSION=.(\d+)' -and [int]$Matches[1] -ge 21){ Write-Output $p; break } } }"`) do set "JDK=%%J"

if not defined JDK (
    echo  [ERRO] Nenhum JDK 21 ou superior foi encontrado.
    echo.
    echo  O projeto compila com release 21. Instale um JDK 21+ ^(por
    echo  exemplo Eclipse Temurin^) e rode este arquivo de novo.
    goto :erro
)

set "JAVA_HOME=%JDK%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo  JDK ......... %JAVA_HOME%

REM ---------------------------------------------------------------------
REM  Servico analitico em Python (opcional)
REM
REM  O jogo funciona sem ele: o backend cai para o calculo equivalente
REM  em Java e registra a origem no relatorio do turno.
REM ---------------------------------------------------------------------
taskkill /FI "WINDOWTITLE eq Complexus - analitico*" /T /F >nul 2>&1

where python >nul 2>&1
if errorlevel 1 (
    echo  Analitico ... Python nao encontrado, o jogo roda sem ele
) else (
    start "Complexus - analitico" /min cmd /c "python "%~dp0analytics\servico_analitico.py""
    echo  Analitico ... porta 8100
)

REM ---------------------------------------------------------------------
REM  Abre o navegador assim que a porta 8080 responder
REM ---------------------------------------------------------------------
start "" /min powershell -NoProfile -ExecutionPolicy Bypass -Command "$fim=(Get-Date).AddMinutes(5); while((Get-Date) -lt $fim){ try{ $s=New-Object Net.Sockets.TcpClient; $s.Connect('127.0.0.1',8080); $s.Close(); Start-Process 'http://localhost:8080'; break } catch { Start-Sleep -Seconds 2 } }"

echo  Backend ..... http://localhost:8080
echo.
echo  A primeira vez demora mais porque o Maven baixa as dependencias.
echo  O navegador abre sozinho quando o servidor estiver pronto.
echo.
echo  Para encerrar: feche esta janela ou pressione Ctrl+C.
echo  ----------------------------------------------------------
echo.

cd backend
call mvn spring-boot:run

REM ---------------------------------------------------------------------
REM  Encerramento: derruba o servico analitico junto
REM ---------------------------------------------------------------------
taskkill /FI "WINDOWTITLE eq Complexus - analitico*" /T /F >nul 2>&1
echo.
echo  Complexus encerrado.
call :pausa
exit /b 0

:erro
echo.
echo  ----------------------------------------------------------
pause
exit /b 1

REM ---------------------------------------------------------------------
REM  Pausa curta antes de fechar a janela.
REM
REM  Chamado pelo caminho absoluto de proposito: em um terminal com as
REM  ferramentas GNU no PATH (Git Bash, por exemplo), "timeout" resolve
REM  para o comando do coreutils, que nao entende /t e quebra aqui.
REM ---------------------------------------------------------------------
:pausa
"%SystemRoot%\System32\timeout.exe" /t 3 /nobreak >nul 2>&1
exit /b 0
