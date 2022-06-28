set SCRIPT_DIR=%~dp0
java -Xms512M -Xmx1536M -Xss1M -jar "%SCRIPT_DIR%sbt-launch.jar" %*
