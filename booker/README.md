# booker

## Run on JVM
Note: You can't run in the sbt shell because jline can't figure out the terminal stuff
```
bin/sbt booker/stage && booker/target/universal/stage/bin/booker
```

Generate GraalVM Config:
1. Install GraalVM: https://github.com/graalvm/graalvm-ce-builds/releases
2. Set the `JAVA_HOME` env var accordingly
3. Download the native-image jar and extract it in `$JAVA_HOME`
4. Add the `$JAVA_HOME/bin` to the front of the `PATH`
5. Make `native-image` executable: `chmod +x $JAVA_HOME/lib/svm/bin/native-image`
6. Create the executable: `bin/sbt booker/stage`
7. Setup the agent: `export JAVA_OPTS="-agentlib:native-image-agent=config-output-dir=booker/src/main/resources/META-INF/native-image"`
8. Run Booker via JVM: `booker/target/universal/stage/bin/booker`
9. Run: `bin/sbt booker/graalvm-native-image:packageBin`