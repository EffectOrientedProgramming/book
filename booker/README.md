# booker

Generate GraalVM Config:
1. Install GraalVM: https://github.com/graalvm/graalvm-ce-builds/releases
2. Set the `JAVA_HOME` env var accordingly
3. Download the native-image jar and extract it in `$JAVA_HOME`
4. Add the `$JAVA_HOME/bin` to the front of the `PATH`
5. Update the GraalVM configs: `bin/sbt booker/run`
6. Make `native-image` executable: `chmod +x $JAVA_HOME/lib/svm/bin/native-image`
7. Run: `bin/sbt booker/graalvm-native-image:packageBin`