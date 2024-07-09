# Installing Scala & Tools

This is an overview of the setup process to install the tools necessary to run the examples from the book. 
The following sections provide detailed instructions.

1.  Download and install [Coursier](https://get-coursier.io/).
    [Coursier](https://get-coursier.io/docs/cli-overview) is the Scala installer we use to ensure that the right JVM and standard Scala tools are installed on your system.
    Details are [here](https://docs.scala-lang.org/scala3/getting-started.html).
1.  Run `cs setup --jvm 21`. This installs the Java Development Kit (JDK) version 21, Scala 3, and support tools such as the Scala Build Tool ([SBT](https://www.scala-sbt.org/)).
1.  Install an Integrated Development Environment (IDE): either [IntelliJ IDEA](https://www.jetbrains.com/help/idea/installation-guide.html) or Visual Studio Code (VSCode).
1.  Download or clone the [examples repository](examples.md).
1.  Open the repository in your IDE.
1.  Install the Scala add-on for your IDE.

## Notes

1. We assume you know how to use the Command-Line Interface (CLI) a.k.a *shell* for your Operating System (OS: Windows, Macintosh or Linux).
If you do not, you can find instructions [here](https://github.com/BruceEckel/AtomicKotlinExamples/blob/master/README.md#appendix-a-command-line-basics).
These instructions were written for [Atomic Kotlin](https://www.atomickotlin.com/), so Kotlin will be referenced.

2. If any terminology or processes described here are still not clear, you can usually find explanations or answers through
[Google](https://www.google.com/). For more specific issues or problems, try [StackOverflow](http://stackoverflow.com/).
Sometimes you can find installation instructions on [YouTube](https://www.youtube.com/).

3. If after any installation step something doesn't seem to work, try closing your shell and opening a new one.

## 1. Install Coursier

**Windows Users:** [Go Here First](https://effectorientedprogramming.com/guide/scalatools/#6-windows-installation)

The process for installing Coursier is different for each operating system.
The [Coursier Installation Instructions](https://get-coursier.io/docs/cli-installation) provide a detailed guide for each system.


## 2. Use Coursier to Install the Required Tools

This command installs the JDK version 21, Scala 3, the SBT build tool, and several other tools.
We specify version 21 of Java on the `cs` command line:

```
cs setup --jvm 21
```

This may take a few minutes. When prompted with a `[Y/n]` query, enter `y` for all options.

## 3. Test Your Installation

Once installation is complete, close the current shell and open a new one. 
You can run a few quick tests to ensure the installation is successful:

1\. `cs java -version`  
  Output will be something like:
```text
openjdk version "21.0.3" 2024-04-16 LTS
OpenJDK Runtime Environment Temurin-21.0.3+9 (build 21.0.3+9-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.3+9 (build 21.0.3+9-LTS, mixed mode, sharing)
```

If your Java version is not 21, update it with:

```text
cs java --jvm 21 --setup
```

2\. `scalac -version`  
  Output will be something like:
```text
Scala compiler version 3.4.2 -- Copyright 2002-2024, LAMP/EPFL
```

3\. `sbt help`  
  Output will be something like:
```text
[info] welcome to sbt 1.10.0 (Eclipse Adoptium Java 21.0.3)
...
```

## 4. Using SBT

In a terminal, move to the directory where you extracted the [examples](examples.md) repository and run:

```
sbt
```

This opens the sbt shell. For this project, the prompt will look something like this:

```
sbt:EffectOrientedProgramming>
```

There are numerous commands available in the sbt command shell.
To see them, type `help` at the sbt prompt.

### Compiling

To compile all the files in the current project, run `compile` in the sbt shell:

```
sbt:EffectOrientedProgramming> compile
```

You might see some warnings the first time you run `compile` after installing the repository but you can ignore them.

### Running a Program

`run` displays a list of all executables in the project:

```
sbt:EffectOrientedProgramming> run
```

To run a specific program, use `runMain`:

```
sbt:EffectOrientedProgramming> runMain programName
```

For example, to run  `App0` from `Chapter03_Superpowers`:

```
sbt:EffectOrientedProgramming> run
(sbt displays numbered list of main programs)
Enter number: 1
```

or

```
sbt:EffectOrientedProgramming> runMain Chapter03_Superpowers.App0
```

> **NOTE: Some output might be out-of-order vs. output shown in the book**.  
> Nodes in the same layer of the dependency graph have arbitrary ordering.
> The ordering of initialization in the dependency graph is only guaranteed in the sense that 'If B depends on A, then A will be constructed first.'
> But whenever there are unrelated pieces, the ordering is arbitrary.
> C could be constructed at any point:  
> - Before A  
> - In between A and B  
> - After B  
> As long as C doesn't depend on A or B.

<!-- Run all programs with a single command: runMainClassesToleratesFailures -->

### Automatic Command Execution

If you precede any command with `~`, sbt automatically runs that command whenever there is a change to the associated files.
For example:

```
sbt:EffectOrientedProgramming> ~runMain Chapter03_Superpowers.App0
```

automatically runs `Chapter03_Superpowers.App0` whenever any of that program's files change.
Pressing the `ENTER` key stops the automated command.

### Exiting

To exit the sbt shell, press **ctrl + d**.

## 5. Troubleshooting

* Downloads can take a long time and might appear to be frozen. Just wait it out.

* If Java is already installed, you might be missing the JDK or using the wrong version of Java. 
  Execute this command to fix the problem:    
  `cs java --jvm 21 --setup`

* Periodically update to the latest Coursier version:

```text
cs update cs
```

* Periodically update your executables by re-installing them, e.g.:
```text
cs install scalafmt
```

<!-- *  `eval "$(cs install --env)"` {{ What does this do? }} -->

## 6. Windows Installation

Coursier might not be able to install Java 21 on your system, so it's much
safer to install Java BEFORE installing Coursier:

1. **Install Chocolatey (if not already installed)**:
    - Open PowerShell as Administrator.
    - Run the following command to install Chocolatey:
      ```
      Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
      ```

2. **Install JDK 21 using Chocolatey**:
   ```
   choco install openjdk --version 21
   ```

Chocolatey also handles environment variable setup. 
Once you finish, close the shell and open a new one.
Verify `JAVA_HOME` and `PATH` variables to ensure they are pointed to the JDK 21 installation.
Now if you install and run Coursier it should find your newly-installed JDK 21 rather than installing a JDK.
