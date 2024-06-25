# Installing Scala & Tools

This is an overview of the setup process to use these examples. The following sections provide detailed instructions.

1.  Download and install [Coursier](https://get-coursier.io/).
    [Coursier](https://get-coursier.io/docs/cli-overview) is the Scala installer we use to ensure that a JVM and standard Scala tools are installed on your system.
    Details are [here](https://docs.scala-lang.org/scala3/getting-started.html).
2.  Run `cs setup`. This installs a Java Development Kit (JDK), Scala 3 {{ Does it? }}, and support tools such as the Scala Build Tool ([SBT](https://www.scala-sbt.org/)).
3.  Install an Integrated Development Environment (IDE): either IntelliJ IDEA or Visual Studio Code (VSCode).
4.  Install the Scala add-on for your IDE.
5.  Clone/download this repository.
6.  Open the `build.sbt` file in your IDE.

## Notes

1. We assume you know how to use the Command-Line Interface (CLI) for your Operating System (OS: Windows, Macintosh or Linux).
If you do not, you can find instructions [here](https://github.com/BruceEckel/AtomicKotlinExamples/blob/master/README.md#appendix-a-command-line-basics).
These instructions were written for [Atomic Kotlin](https://www.atomickotlin.com/), so Kotlin will be referenced.

2. If any terminology or processes described here are still not clear, you can usually find explanations or answers through
[Google](https://www.google.com/). For more specific issues or problems, try
[StackOverflow](http://stackoverflow.com/). Sometimes you can find installation
instructions on [YouTube](https://www.youtube.com/).

3. If after any installation step something doesn't seem to work, try closing your shell and opening a new one.

## 1. Install Coursier

The process for installing Coursier is different for each operating system.

### Windows

Run the following commands (**Note:** Use cmd.exe, not Powershell):

```
> bitsadmin /transfer cs-cli https://git.io/coursier-cli-windows-exe "%cd%\cs.exe"
> .\cs --help
```

### Macintosh

Run the following commands:

```
> curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)"
> chmod +x cs
> ./cs install cs
```

### Linux

Use `curl` to install **Coursier**:

```
> curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)"
> cs
```
Running the `cs` command verifies that Coursier was installed. 
Now open a fresh terminal before proceeding.

If the terminal displays a message saying 'cs is not found',
check the previous command. Otherwise, enter these
commands to give execute permission to Coursier and install the
package:

```
> chmod +x cs
> ./cs install cs
> eval "$(cs install --env)"
```

If the terminal displays a message like:

```
Warning: /home/ExamplePath/Example/coursier/bin is not in your PATH
```

Use the following command, replacing the `<Insert Path>` with the path from the
above warning message:

```
> export PATH="$PATH:/<Insert Path>/coursier/bin"
```

For example, if the warning message is:

```
Warning: /home/bob/.local/share/coursier/bin is not in your PATH
```

Run this:

```
> export PATH="$PATH:/home/bob/.local/share/coursier/bin"
```

You'll want to put a line like this in your `~/.bashrc`, as well.

Then enter these commands to give execute permission to Coursier and install the
package:

```
> chmod +x cs
> ./cs install cs
> eval "$(cs install --env)"
```

## 2. Use Coursier to Install the Required Tools

This command installs the JDK, Scala 3, the SBT build tool, and several other tools:

```
> cs setup
```

This may take a few minutes. When prompted with a `[Y/n]` query, enter `y` for all options.

Once installation is complete, close the current shell and open a new one. 
Now you can run a few simple tests to ensure the installation was successful:

1. `java -version`
  Output will be something like:
  ```text
  java version "1.8.0_211"
  Java(TM) SE Runtime Environment (build 1.8.0_211-b12)
  Java HotSpot(TM) 64-Bit Server VM (build 25.211-b12, mixed mode)
  ```

2. `javac -version`
  Output will be something like:
  ```text
  javac 1.8.0_211
  ```

3. `scalac -version`
  Output will be something like:
  ```text
  Scala compiler version 2.13.6 -- Copyright 2002-2021, LAMP/EPFL and Lightbend, Inc.
  ```

4. `sbt help`
  Output will be something like:
  ```text
  [info] welcome to sbt 1.5.5 (Oracle Corporation Java 1.8.0_211)
  [info] loading settings for project book-build from plugins.sbt ...
  [info] loading project definition from C:\Users\bruce\Documents\Git\book\projec
  [info] loading settings for project book from build.sbt ...
  [info] set current project to EffectOrientedProgramming
  ...
  ```

### Update Coursier

After Coursier is installed, it can be updated with:

```
> cs update cs
```

Do this periodically to make sure you have the latest Coursier version.

## 3. Using SBT

In a terminal, run the command:

```
> sbt
```

This opens the sbt shell. For this project, the prompt will look like this:

```
sbt:EffectOrientedProgramming>
```

From here, you can run the programs in your project.

### Compiling

To compile all the files in the current project, run the following inside the sbt shell:

```
sbt:EffectOrientedProgramming> compile
```

### Running a Program

(All commands here are run from within the sbt shell)

To display a list of all executables in the project:

```
sbt:EffectOrientedProgramming> run
```

To run a specific program, use `runMain`:

```
sbt:EffectOrientedProgramming> runMain programName
```

For example, the main function of the `HelloWorld` object is located in
the directory `Examples`. To run `helloWorld`:

```
sbt:EffectOrientedProgramming> run
(sbt displays number list of main programs)
Enter number: (input the index of helloWorld)
```

or

```
sbt:EffectOrientedProgramming> runMain helloWorld
```

To run a program in a package, use the format **runMain packagename.mainName**.

### Automatic Command Execution

If you precede any command with `~`, sbt automatically runs that command whenever there is a change to the associated files.
For example:

```
sbt:EffectOrientedProgramming> ~runMain helloWorld
```

automatically runs `helloWorld` whenever any of that program's files change.
Pressing the `ENTER` key stops the automated command.

### Exiting

To exit the sbt shell, press **ctrl + d**.

## 4. Misc Recommendations

* Downloads can take a long time and might appear to be frozen. Just wait it out.

*  `eval "$(cs install --env)"` {{ What does this do? }}

* If Java is already installed, you might be missing the JDK, so execute 
  this command to be sure:

  `cs java --jvm adopt:11 --setup`

* Periodically update your exectuables by re-installing them, e.g.:

  `cs install scalafmt`
