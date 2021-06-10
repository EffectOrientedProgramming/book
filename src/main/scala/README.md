# Effect Oriented Programming Examples

If you want to experiment with the code examples from the book [Effect Oriented Programming](https://github.com/EffectOrientedProgramming/book), you're in the right place.

These examples are automatically extracted directly from the book. This repository
includes tests to verify that the code in the book is correct.

!!!!!!
> **NOTE** If you are planning to solve the exercises after reading each atom
> (and you should), you can get the exercises AND all these examples together by
> installing [the educational course](https://www.atomickotlin.com/exercises/).
> If you're going to solve the exercises, you can just install the course and you
> don't need to install the examples from this repository.

!!!!!!



## Contents
- [Introduction](#introduction)
- [Command-Line Basics](#command-line-basics)
    * [Editors](#editors)
    * [The Shell](#the-shell)
        + [Starting a Shell](#starting-a-shell)
        + [Directories](#directories)
        + [Basic Shell Operations](#basic-shell-operations)
        + [Unpacking a Zip Archive](#unpacking-a-zip-archive)
- [Installation](#installation)
    * [Install Java](#install-java)
        + [Windows](#windows-java))
        + [Macintosh](#macintosh-java)
        + [Linux](#linux-java)
    * [Verify Your Installation](#verify-your-installation)
    * [Install Coursier](#install-coursier)
        + [Windows](#windowscs)
        + [Macintosh](#macintoshcs)
        + [Linux](#linuxcs)
    * [Installing and Running the Book Examples](#installing-and-running-the-book-examples)
- [Compiling and Running Programs in IntelliJ IDEA](#compiling-and-running-programs-in-intellij-idea)
- [Appendix B: Command-Line Hello World](#appendix-b-command-line-hello-world)
    * [Packages](#packages)
- [Appendix C: The Kotlin REPL](#appendix-c-the-kotlin-repl)
    * [Install Kotlin](#install-kotlin)
    * [The REPL](#the-repl)
- [Appendix D: Testing](#appendix-d-testing)

# Introduction

The easiest way to access and experiment with the book examples is to
clone/download this repository and open it with IntelliJ IDEA. This README will also cover how to 
download [Coursier](https://get-coursier.io/), which will be very helpful when using [SBT](https://www.scala-sbt.org/).

The remainder of this README shows you how to build and test the examples using
 IntelliJ IDEA, SBT, and the command line.

!!!!!
Exercises and solutions for the book can be found at
[AtomicKotlin.com/exercises](https://www.atomickotlin.com/exercises).

**Note**: If any terminology or processes described here are still not clear to
you, you can usually find explanations or answers through
[Google](https://www.google.com/). For more specific issues or problems, try
[StackOverflow](http://stackoverflow.com/). Sometimes you can find installation
instructions on [YouTube](https://www.youtube.com/).

# Command-Line Basics

Because it is possible for a "dedicated beginner" to learn programming from
this book, you may not have previously used your computer's command-line shell.
If you have, you can go directly to the
[installation instructions](#building-from-the-command-line-detailed-instructions).

## Editors

To create and modify Kotlin program files&mdash;the code listings shown in this
book&mdash;you need a program called an *editor*. You'll also need the editor to
make changes to your system configuration files, which is sometimes required
during installation.

Programming editors vary from heavyweight *Integrated Development Environments*
(IDEs, like Eclipse, NetBeans and IntelliJ IDEA) to more basic text
manipulation applications. If you already have an IDE and are comfortable with
it, feel free to use that for this book.

Numerous explanations in this book are specific to IntelliJ IDEA so if you
don't already have an IDE you might as well start with IDEA. There are many
other editors; these are a subculture unto themselves and people sometimes get
into heated arguments about their merits. If you find one you like better, it's
not too hard to change. The important thing is to choose one and get
comfortable with it.

## The Shell

If you haven't programmed before, you might be unfamiliar with your operating
system *shell* (also called the *command prompt* in Windows). The shell harkens
back to the early days of computing when everything happened by typing commands
and the computer responded by displaying responses&mdash;everything was text-based.

Although it can seem primitive in the age of graphical user interfaces (GUI's), a shell
provides a surprising number of valuable features.

To learn more about your shell than we cover here, see
[Bash Shell](https://en.wikipedia.org/wiki/Bash_(Unix_shell)) for Mac/Linux
or [Windows Shell](https://en.wikipedia.org/wiki/Windows_shell).

### Starting a Shell

**Mac**: Click on the *Spotlight* (the magnifying-glass icon in the upper-right
corner of the screen) and type "terminal." Click on the application that looks
like a little TV screen (you might also be able to hit "Return"). This starts a
shell in your home directory.

**Windows**: First, start the Windows Explorer to navigate through your
directories:

- *Windows 7*: click the "Start" button in the lower left corner of the screen.
  In the Start Menu search box area type "explorer" and then press the "Enter"
  key.

- *Windows 8*: click Windows+Q, type "explorer" and then press the "Enter" key.

- *Windows 10*: click Windows+E.

Once the Windows Explorer is running, move through the folders on your computer
by double-clicking on them with the mouse. Navigate to the desired folder. Now
click the file tab at the top left of the Explorer window and select "Open
Windows Powershell." This opens a shell in the destination directory.

**Linux**: To open a shell in your home directory:

- *Debian*: Press Alt+F2. In the dialog that pops up, type 'gnome-terminal'

- *Ubuntu*: Either right-click on the desktop and select 'Open Terminal', or
  press Ctrl+Alt+T

- *Redhat*: Right-click on the desktop and select 'Open Terminal'

- *Fedora*: Press Alt+F2. In the dialog that pops up, type 'gnome-terminal'


### Directories

*Directories* are one of the fundamental elements of a shell. Directories hold
files, as well as other directories. Think of a directory as a tree with
branches. If `books` is a directory on your system and it has two other
directories as branches, for example `math` and `art`, we say that you have a
directory `books` with two *subdirectories* `math` and `art`. We refer to them
as `books/math` and `books/art` since `books` is their *parent* directory.
Note that Windows uses backslashes rather than forward slashes to separate the
parts of a directory.

### Basic Shell Operations

The shell operations shown here are approximately identical across operating
systems. For the purposes of this book, here are the essential operations in a
shell:

-   **Change directory**: Use `cd` followed by the name of the
    directory where you want to move, or `cd ..` if you want to move
    up a directory. If you want to move to a different directory while
    remembering where you came from, use `pushd` followed by the different
    directory name. Then, to return to the previous directory, just say
    `popd`.

-   **Directory listing**: `ls` (`dir` in Windows) displays all the files and
    subdirectory names in the current directory. Use the wildcard `*` (asterisk) to
    narrow your search. For example, if you want to list all the files ending in
    ".kt," you say `ls *.kt` (Windows: `dir *.kt`). If you want to list the
    files starting with "F" and ending in ".kt," you say `ls F*.kt` (Windows:
    `dir F*.kt`).

-   **Create a directory**: use the `mkdir` ("make directory") command
    (Windows: `md`), followed by the name of the directory you want to create.
    For example, `mkdir books` (Windows: `md books`).

-   **Remove a file**: Use `rm` ("remove") followed by the name of the file
    you wish to remove (Windows: `del`). For example, `rm somefile.kt` (Windows:
    `del somefile.kt`).

-   **Remove a directory**: use the `rm -r` command to remove the files in
    the directory and the directory itself (Windows: `deltree`). For example,
    `rm -r books` (Windows: `deltree books`).

-   **Repeat a command**: The "up arrow" on all three operating
    systems moves through previous commands so you can edit and
    repeat them. On Mac/Linux, `!!` repeats the last command and
    `!n` repeats the nth command.

-   **Command history**: Use `history` in Mac/Linux or press the F7 key in Windows.
    This gives you a list of all the commands you've entered. Mac/Linux provides
    numbers to refer to when you want to repeat a command.

### Unpacking a Zip Archive

A file name ending with `.zip` is an archive containing other files in a
compressed format. Both Linux and Mac have command-line `unzip` utilities, and
it's possible to install a command-line `unzip` for Windows via the Internet.

However, in all three systems the graphical file browser (Windows Explorer, the
Mac Finder, or Nautilus or equivalent on Linux) will browse to the directory
containing your zip file. Then right-mouse-click on the file and select "Open"
on the Mac, "Extract Here" on Linux, or "Extract all ..." on Windows.


# Installation 


## Install Java

Scala runs on top of Java, so you must first install Java version 1.6 or later
(you only need basic Java; the development kit also works but is not
required). In this book we use JDK8 (Java 1.8).


### Windows Java

Follow the instructions in `The Shell` to open a Powershell. 
Run **java -version** at the prompt (regardless of the subdirectory you’re in) to see if
   Java is installed on your computer. If it is, you see something like the
   following (sub-version numbers and actual text will vary):

>java version "1.8.0_11"  <br />
>Java(TM) SE Runtime Environment (build 1.8.0_25-b18) <br />
>Java HotSpot(TM) 64-Bit Server VM (build 25.25-b02, mixed mode)

   If you have at least Version 6 (also known as Java 1.6), you do not need to
   update Java.

   If you need to install Java, first determine whether you’re running 32-bit or
   64-bit Windows.

   **In Windows 7**, go to “Control Panel,” then “System and Security,” then
   “System.” Under “System,” you see “System type,” which will say either “32-
   bit Operating System” or “64-bit Operating System.

   **In Windows 8**, press the Windows+W keys, and then type “System” and
   press “Return” to open the System application. Look for “System Type,”
   which will say either “32-bit Operating System” or “64-bit Operating
   System.”

   To install Java, follow the instructions here:
>java.com/en/download/manual.jsp

   This attempts to detect whether to install a 32-bit or 64-bit version of Java,
   but you can manually choose the correct version if necessary.

   After installation, close all installation windows by pressing “OK,” and then
   verify the Java installation by closing your old Powershell and running **java
   -version** in a new Powershell.
   
###Set the Path
   If your system still can’t run **java -version** in Powershell, you must add the
   appropriate **bin** directory to your *path*. The path tells the operating system
   where to find executable programs. For example, something like this goes
   at the end of the path:
>;C:\Program Files\Java\jre8\bin

   This assumes the default location for the installation of Java. If you put it
   somewhere else, use that path. Note the semicolon at the beginning – this
   separates the new directory from previous path directives.\

   **In Windows 7**, go to the control panel, select “System,” then “Advanced
   System Settings,” then “Environment Variables.” Under “System variables,”
   open or create **Path**, then add the installation directory “bin” folder shown
   above to the end of the “Variable value” string.

   **In Windows 8**, press Windows+W, then type **env** in the edit box, and
   choose “Edit Environment Variables for your account.” Choose “Path,” if it
   exists already, or add a new **Path** environment variable if it does not. Then
   add the installation directory “bin” folder shown above to the end of the
   “Variable value” string for **Path.**

   Close your old Powershell window and start a new one to see the change.
   
###Install Scala
   In this book, we use Scala version 2.11, the latest available at the time. In
   general, the code in this book should also work on versions more recent
   than 2.11.

   The main download site for Scala is:
   
> www.scala-lang.org/downloads

   Choose the MSI installer which is custom-made for Windows. Once it
   downloads, execute the resulting file by double-clicking on it, then follow
   the instructions.

   Note: If you are running Windows 8, you might see a message that says
   “Windows SmartScreen prevented an unrecognized app from starting.
   Running this app might put your PC at risk.” Choose “More info” and then
   “Run anyway.”

   When you look in the default installation directory (**C:\Program Files
   (x86)\scala** or **C:\Program Files\scala**), it should contain:
   > bin     doc     lib     api

   The installer will automatically add the **bin** directory to your path.
   
   Now open a new Powershell and type
   > scala -version

   at the Powershell prompt. You’ll see the version information for your Scala
   installation.


!!!!!
###Source Code for the Book
   We include a way to easily test the Scala exercises in this book with a
   minimum of configuration and download. Follow the links for the book’s
   source code at AtomicScala.com and download the package (this places it
   in your “Downloads” directory unless you have configured your system to
   place it elsewhere).
   To unpack the book’s source code, locate the file using the Windows
   explorer, then right-click on atomic-scala-examples-master.zip and
   choose “Extract all …” then choose the default destination folder. Once
   everything is extracted, move into the destination folder and navigate
   down until you find the examples directory.
   Move to the C:\ directory and create the C:\AtomicScala directory. Either
   copy or drag the examples directory into the C:\AtomicScala directory.
   Now the AtomicScala directory contains all the examples from the book. 

###Set Your CLASSPATH
   To run the examples, you must first set your CLASSPATH, an environment
   variable used by Java (Scala runs atop Java) to locate code files. If you want
   to run code files from a particular directory, you must add that new
   directory to the CLASSPATH.

   **In Windows 7**, go to “Control Panel,” then “System and Security,” then
   “System,” then “Advanced System Settings,” and finally “Environment
   Variables.”

   **In Windows 8**, open Settings with Windows-W, type “env” in the edit box,
   then choose “Edit Environment Variables for your account.”
   Under “System variables,” open “CLASSPATH,” or create it if it doesn’t exist.
   Then add to the end of the “Variable value” string:
   >;C:\AtomicScala\examples

   This assumes the aforementioned location for the installation of the Atomic
   Scala code. If you put it somewhere else, use that path.
   Open a Powershell window, change to the **C:\AtomicScala\examples**
   subdirectory, and run:
   > scalac AtomicTest.scala

   If everything is configured correctly, this creates a subdirectory
   com\atomicscala that includes several files, including:
   >AtomicTest$.class <br />
   >AtomicTest.class

   The source-code download package from **AtomicScala.com** includes a
   Powershell script, **testall.ps1**, to test all the code in the book using
   Windows. Before you run the script for the first time, you must tell
   Powershell that it’s OK. In addition to setting the Execution Policy as
   described in `The Shell`, you must unblock the script. Using the Windows
   Explorer, go to the **C:\AtomicScala\examples** directory. Right click on
   **testall.ps1**, choose “Properties” and then check “Unblock.”

   Running: 
   >./testall.ps1 
   
   tests all the code examples from the book. You get a
   couple of errors when you do this and that’s fine; it’s due to things that we
   explain later in the book.

### Macintosh Java

The Mac comes with a much older version of Java that won't work for the
examples in this book, so you'll need to update it to (at least) Java 8.

1.  Follow the instructions at this link to [Install HomeBrew](http://brew.sh/)

2.  At a [shell prompt](#appendix-a-command-line-basics), first type
    `brew update`. When that completes, enter `brew cask install java`.

**NOTE:** Sometimes the default version of Java that you get with the above
installation will be too recent and not validated by the Mac's security
system. If this happens you'll either need to turn off the security by hand
or install an earlier version of Java. For either choice, you'll need to Google
for answers on how to solve the problem (often the easiest approach is to just
search for the error message produced by the Mac).

### Linux Java

Use the standard package installer with the following [shell commands](#appendix-a-command-line-basics):

*Ubuntu/Debian*:

1. `sudo apt-get update`

2. `sudo apt-get install default-jdk`

*Fedora/Redhat*:

```
su -c "yum install java-1.8.0-openjdk"
```

## Verify Your Installation

[Open a new shell](#appendix-a-command-line-basics) and type:

```
java -version
```

You should see something like the following (Version numbers and actual text
will vary):

```
openjdk version "11" 2018-09-25
OpenJDK Runtime Environment 18.9 (build 11+28)
OpenJDK 64-Bit Server VM 18.9 (build 11+28, mixed mode)
```

If you see a message that the command is not found or not recognized, review
the installation instructions. If you still can't get it to work, check
[StackOverflow](http://stackoverflow.com/search?q=installing+java).

##Install Coursier

###Windows Coursier

To download **Coursier**, use the following instructions:

> bitsadmin /transfer cs-cli https://git.io/coursier-cli-windows-exe "%cd%\cs.exe"
> 
> .\cs --help

**Note:** These commands must be used with cmd.exe, not Powershell.

To update **Coursier** in the future, use the command:
> cs update cs

###Macintosh Coursier

To download **Coursier**, and set the **PATH**, use the following instructions:
> curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)" <br />
>
> chmod +x cs <br />
>
> ./cs install cs <br />
>
> rm cs


To update **Coursier** in the future, use the command:
> cs update cs

###Linux Coursier

To download **Coursier**, and set the **PATH**, use the following instructions:
> curl -fLo cs https://git.io/coursier-cli-"$(uname | tr LD ld)" <br />
> 
> chmod +x cs <br />
> 
> ./cs install cs <br />
> 
> rm cs


To update **Coursier** in the future, use the command:
> cs update cs

### Use Coursier to Download Several Useful Applications (All OS)

To install several usefull applications, including the SBT system, which will be very useful 
later, simply run the command:
> cs

This will trigger several downloads that may take a few minutes. 

## Installing and Running the Book Examples

Once you have Java installed, the process to install and run the book examples
is the same for all platforms:

1. Download the book examples from the
   [GitHub Repository](https://github.com/BruceEckel/AtomicKotlinExamples/archive/master.zip).

2. [Unzip](#unpacking-a-zip-archive) the downloaded file into the directory of your choice.

3. Use the Windows Explorer, the Mac Finder, or Nautilus or equivalent on Linux
   to browse to the directory where you uzipped `AtomicKotlinExamples`, and
   [open a shell](#appendix-a-command-line-basics) there.

4. If you're in the right directory, you should see files named `gradlew` and
   `gradlew.bat` in that directory, along with numerous other files and
   directories. The directories correspond to the chapters in the book.

5. At the shell prompt, type `gradlew test` (Windows) or `./gradlew test`
   (Mac/Linux).

The first time you do this, Gradle will install itself and numerous other
packages, so it will take some time. After everything is installed, subsequent
builds and runs will be much faster.

Note that you must be connected to the Internet the first time you run `gradlew`
so that Gradle can download the necessary packages.


# Compiling and Running Programs in IntelliJ IDEA

The easiest and fastest way to start using the examples in this book is by
compiling and running them using IntelliJ IDEA:

1. Follow the instructions [here](https://www.jetbrains.com/help/idea/installation-guide.html)
   to install IntelliJ IDEA.

!!!!!!
2. Download the [zipped code
   repository](https://github.com/BruceEckel/AtomicKotlinExamples/archive/master.zip)
   and [unzip it](#unpacking-a-zip-archive).

3. Start IntelliJ IDEA and select the `File | Open` menu item. Navigate to
   where you unzipped the repository and open the `build.gradle` file. You should
   see a dialog box like this:

   ![](images/buildgradle.png)

   Select the `Open as Project` button.

4. If you don't see a `Project` window on the left side, go to the menu and select
   `View | Tool Windows | Project` to turn it on.

5. You'll see an `Examples` folder. Click on it to open it, then navigate to
   the `HelloWorld` folder and open that, then double-click on `HelloWorld.kt`.
   You'll see something like this:

   ![](images/helloworld.png)

   Click on the green triangle in the gutter area to the left of `fun main() {`.
   It should look like this:

   ![](images/runhelloworld.png)

   Select the top one, the `Run` option, and IntelliJ IDEA will run your
   program and display the resulting output.

   **NOTE**: The first program you run will take awhile, because IntelliJ IDEA
   is building the entire project. Subsequent programs will start much more
   quickly.

6. If you don't already have a JDK (*Java Development Kit*) on your machine,
   you will see error messages. A JDK is necessary to compile both Java and
   Kotlin. You can [install one from within
   IntelliJ](https://www.jetbrains.com/help/idea/sdk.html#jdk-from-ide). Once the
   JDK is installed, IDEA will also be able to compile Kotlin.
   




# Appendix B: Command-Line Hello World

This appendix explains how to compile and run the program shown in the "Hello
World" atom in the book, using the latest version (1.4 or higher) of the
[Kotlin command-line compiler](http://kotlinlang.org/docs/tutorials/command-line.html).

Open up a console window in the `HelloWorld` directory, where you'll see
`HelloWorld.kt`, and type:

```
kotlinc HelloWorld.kt
```

`kotlinc` means "Kotlin compiler." The compiler is the program that takes
your program and turns it into something that will run; this process is
called *compiling*.

Assuming you've typed the code correctly, you should get back the console
prompt, with no other messages. If you get error messages, try to discover
where you've mis-typed the code, correct it and try again. Once you are
successful, you're ready to run the program.

There's one more thing: When you run `kotlinc`, the resulting program doesn't
have the same name as the source program. Instead, the compiler appends a `Kt`
to the name. To see it, run `ls` or `dir` on the `helloworld` subdirectory.
You'll see that the directory contains `HelloWorldKt.class`. What's important
is the part before the `.class`. This is the actual name of the program:
`HelloWorldKt`.

Now we can run the program:

```
kotlin HelloWorldKt
```

And you'll see the output on the console:

```
Hello, world!
```

## Packages

If the program is in a package, the package name is also required to run the
program. That is, if `Foo.kt` contains a `package` statement:

```
package bar
```

then you cannot simply say:

```
kotlin Foo
```

You'll get a message starting with `error: could not find or load`...

If you were to compile this program, you'd see there's a new subdirectory
called `bar`. The name of the subdirectory that appears when you run `kotlinc`
corresponds to the `package` name in the program that was compiled.

If the program is packaged under `bar`, we give the package name followed by a
"dot," then the program's name:

```
kotlin bar.FooKt
```

# Appendix C: The Kotlin REPL

The Kotlin interpreter is also called the REPL (for *Read-Evaluate-Print-
Loop*). To use this you must first install the
latest version (1.4 or higher) of the [Kotlin command-line
compiler](http://kotlinlang.org/docs/tutorials/command-line.html).

> NOTE: You do not need to install command-line Kotlin for the operations
> described previously in this README.

## Install Kotlin

In this book, we use Kotlin version 1.4, the latest available at the time. The
detailed installation instructions for the command-line compiler are available
at [The Kotlin Site](https://kotlinlang.org/docs/tutorials/command-line.html).

To check your installation, open a new shell and type:

```
kotlin -version
```

at the shell prompt. You'll see the version information for your Kotlin
installation.

## The REPL

To start the REPL, type `kotlinc` by itself on the command line. You should see
something like the following:

```
Welcome to Kotlin version 1.4 (JRE 1.8.0_144-b01)
Type :help for help, :quit for quit
>>>
```

The exact version numbers will vary depending on the versions of Kotlin
and Java you've installed, but make sure that you're running Kotlin 1.4
or greater.

The REPL gives you immediate interactive feedback, which is helpful for
experimentation. For example, you can do arithmetic:

```
>>> 42 * 11.3
474.6
```

Find out more by typing `:help` at the Kotlin prompt. To exit the REPL, type:

```
>>> :quit
```

To compile and run examples using the Kotlin command-line tools, see
[Command-Line Hello World](#appendix-b-command-line-hello-world).

# Appendix D: Testing

The test system is built in so that we (the authors) can verify the correctness
of what goes into the book.

You don't need to run the tests, but if you want to, you can just run `gradlew
test` (on Windows) or `./gradlew test` (Mac/Linux).

There are two steps in creating and running the tests, which you can run
separately if you want (again, just running the Gradle `test` command will
validate the code, so you don't need to do the following steps):

1. `gradlew GenerateTests` generates tests from the sources in this repository.
   It creates (or recreates) the file `TestExamples.java`. You normally don't need to run this; the
   `TestExamples.java` in the repository should be up to date.

2. `gradlew TestExamples` runs the tests in `TestExamples.java`.

Alternatively, `TestExamples.java` can be called as a regular **JUnit** test class.