@echo off
set BASE_DIR=%~dp0..\..
set TS=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TS=%TS: =0%
if not exist "%BASE_DIR%eports" mkdir "%BASE_DIR%eports"

jmeter -n ^
  -t "%BASE_DIR%\jmeter_core_seckill_token_disabled.jmx" ^
  -l "%BASE_DIR%eports\core_%TS%.jtl" ^
  -e -o "%BASE_DIR%eports\html_core_%TS%" ^
  -Jhost=127.0.0.1 ^
  -Jport=8081 ^
  -JactivityId=9001 ^
  -Jthreads=200 ^
  -JrampUp=10 ^
  -Jloops=1 ^
  -JbaseUserId=300000

python "%BASE_DIR%	oolsnalyze_jmeter_result.py" "%BASE_DIR%eports\core_%TS%.jtl" "%BASE_DIR%eports\core_%TS%_summary.md"
