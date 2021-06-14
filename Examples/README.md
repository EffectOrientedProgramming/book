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
    * [Windows](#windows)
    * [Macintosh](#macintosh)
    * [Linux](#linux)
    * [Coursier](#install-coursier)
- [Introduction to Scala](#introduction-to-scala)
- [Compiling and Running Programs in IntelliJ IDEA](#compiling-and-running-programs-in-intellij-idea)
- [Appendix B: Command-Line Hello World](#appendix-b-command-line-hello-world)
    * [Packages](#packages)
- [Appendix C: Testing](#appendix-d-testing)

# Introduction

The easiest way to access and experiment with the book examples is to
clone/download this repository and open it with IntelliJ IDEA. This README will also cover how to 
download [Coursier](https://get-coursier.io/), which will be very helpful when using [SBT](https://www.scala-sbt.org/).

The remainder of this README shows you how to build and test the provided examples using
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
[installation instructions](#installations).

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


## Windows

Scala runs on top of Java, so you must first install Java version 1.6 or later
(you only need basic Java; the development kit also works but is not
required). In this book we use JDK8 (Java 1.8). !!!!(JDK8 ?)

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
   than 2.11. !!!! (version 2.11?)

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

## Macintosh
Scala runs atop Java, and the Mac comes with Java pre-installed. Use
**Software Update** on the Apple menu to check that you have the most up-to-date version of Java for your Mac, and update it if necessary. You need
at least Java version 1.6. It is not necessary to update your Mac operating
system software. In this book we use JDK8 (Java 1.8).

Follow the instructions in `The Shell` to open a shell in the desired directory.
Now type **java -version** at the prompt (regardless of the subdirectory
you’re in) and see the version of Java installed on your computer. You
should see something like the following (version numbers and actual text
will vary):

>java version "1.6.0_37" <br />
>Java(TM) SE Runtime Environment (build 1.6.0_37-b06-434-
10M3909)<br />
>Java HotSpot(TM) 64-Bit Server VM (build 20.12-b01-434,
mixed mode)<br />

If you see a message that the command is not found or not recognized,
there’s a problem with your Mac. Java should always be available in the
shell.

## Install Scala
In this book, we use Scala version 2.11, the latest available at the time. In
general, the code in this book should also work on versions more recent
than 2.11.

The main download site for Scala is:
> www.scala-lang.org/downloads

Download the version with the **.tgz** extension. Click on the link on the web
page, then select “open with archive utility.” This puts it in your
“Downloads” directory and un-archives the file into a folder. (If you
download without opening, open a new Finder window, right-click on the
**.tgz** file, then choose “Open With -> Archive Utility”).

Rename the un-archived folder to “Scala” and then drag it to your home
directory (the directory with an icon of a home, and is named whatever
your user name is). If you don’t see a home icon, open “Finder,” choose
“Preferences” and then choose the “Sidebar” icon. Check the box with the
home icon next to your name in the list.

When you look at your **Scala** directory, it should contain:

> bin doc examples lib man misc src

###Set the Path
Now add the appropriate **bin** directory to your *path*. Your path is usually
stored in a file called **.profile** or **.bash_profile**, located in your home
directory. We assume that you’re editing **.bash_profile** from this point
forward.

If neither file exists, create an empty file by typing:
> touch ~/.bash_profile

Update your path by editing this file. Type:

>open ~/.bash_profile.

Add the following at the end of all other PATH statement lines:
>PATH="$HOME/Scala/bin/:${PATH}" <br />
>export PATH

By putting this at the end of the other PATH statements, when the
computer searches for Scala it will find your version of Scala first, rather
than others that can exist elsewhere in the path.

From that same terminal window, type:
>source ~/.bash_profile

Now open a new shell and type
>scala -version

at the shell prompt. You’ll see the version information for your Scala
installation.


!!!!
###Source Code for the Book
We include a way to easily test the Scala exercises in this book with a
minimum of configuration and download. Follow the links for the book’s
source code at **AtomicScala.com** and download **atomic-scala-examples-master.zip** into a convenient location on your computer.

Unpack the book’s source code by double clicking on atomic-scala-examples-master.zip. Navigate down into the resulting unpacked folder
until you find the examples directory. Create an AtomicScala directory in
your home directory, and drag examples into the AtomicScala directory,
using the directions above (for installing Scala).

The ~/AtomicScala directory now contains all the examples from the book
in the subdirectory examples.

###Set Your CLASSPATH
The CLASSPATH is an environment variable used by Java (Scala runs atop
Java) to locate Java program files. If you want to place code files in a new
directory, you must add that new directory to the CLASSPATH.
Edit your ~/.profile or ~/.bash_profile, depending on where your path
information is located, and add the following:
CLASSPATH="$HOME/AtomicScala/examples:${CLASSPATH}"
export CLASSPATH
Open a new terminal window and change to the AtomicScala subdirectory
by typing:
cd ~/AtomicScala/examples
Now run:
scalac AtomicTest.scala
If everything is configured correctly, this creates a subdirectory
com/atomicscala that includes several files, including:
AtomicTest$.class
AtomicTest.class40 • Atomic Scala • Installation (Mac)
Finally, test all the code in the book by running the testall.sh file that you
find there (part of the book’s source-code download from
AtomicScala.com) with:
chmod +x testall.sh
./testall.sh
You get a couple of errors when you do this and that’s fine; it’s due to
things that we explain later in the book.


### Linux

In this book, we use Scala version 2.11, the latest available at the time. In
general, the examples in this book should also work on versions more
recent than 2.11.

###Standard Package Installation
**Important**: The standard package installer might not install the most
recent version of Scala. There is often a significant delay between a release
of Scala and its inclusion in the standard packages. If the resulting version
is not what you need, follow the instructions in the section titled “Install
Recent Version From tgz File.”

Ordinarily, you can use the standard package installer, which also installs
Java if necessary, using one of the following shell commands (see  `The
Shell`):

*Ubuntu/Debian:* 
>sudo apt-get install scala

*Fedora/Redhat* (release 17+): 
>sudo yum install scala

(Prior to release 17, Fedora/Redhat contains an old version of Scala,
incompatible with this book).

Now follow the instructions in the next section to ensure that both Java
and Scala are installed and that you have the right versions.

###Verify Your Installation
Open a shell (see `The Shell`) and type “**java -version**” at the prompt. You
should see something like the following (Version numbers and actual text
will vary):

>java version "1.7.0_09" <br />
>Java(TM) SE Runtime Environment (build 1.7.0_09-b05) <br />
>Java HotSpot(TM) Client VM (build 23.5-b02, mixed mode) <br />

If you see a message that the command is not found or not recognized,
add the java directory to the computer’s execution path using the
instructions in the section “Set the Path.”

Test the Scala installation by starting a shell and typing “scala -version.”
This should produce Scala version information; if it doesn’t, add Scala to
your path using the following instructions.

###Configure your Editor
If you already have an editor that you like, skip this section. If you chose to
install Sublime Text 2, as we described in `Editors`, you must tell Linux where
to find the editor. Assuming you have installed Sublime Text 2 in your
home directory, create a symbolic link with the shell command:

>sudo ln -s ~/"Sublime Text 2"/sublime_text/usr/local/bin/sublime

This allows you to edit a file named **filename** using the command:
> sublime filename

###Set the Path
If your system can’t run **java -version** or **scala -version** from the console
(terminal) command line, you might need to add the appropriate **bin**
directory to your path.

Your path is usually stored in a file called **.profile** located in your home
directory. We assume that you’re editing **.profile** from this point forward.

Run **ls -a** to see if the file exists. If not, create a new file using the sublime
editor, as described above, by running:

>sublime ~/.profile.

Java is typically installed in **/usr/bin**. Add Java’s **bin** directory to your path
if your location is different. The following **PATH** directive includes both
**/user/bin** (for Java) and Scala’s **bin**, assuming your Scala is in a **Scala**
subdirectory off of your home directory (note that we use a fully qualified
path name – not ~ or **$HOME** – for your home directory):

>export PATH=/usr/bin:/home/`whoami`/Scala/bin/:$PATH:

`whoami` (note the back quotes) inserts your username.

**Note**: Add this line at the end of the **.profile** file, after any other lines that
set the **PATH**.

Next, type:

> source ~/.profile

to get the new settings (or close your shell and open a new one). Now
open a new shell and type

> scala -version

at the shell prompt. You’ll see the version information for your Scala
installation.

If you get the desired version information from both **java -version** and
**scala -version**, skip the next section.

###Install Recent Version from tgz File
Try running **java -version** to see if you already have Java 1.6 or greater
installed. If not, go to **www.java.com/getjava**, click “Free Java Download”
and scroll down to the download for “Linux” (there is also a “Linux RPM”
but we just use the regular version). Start the download and ensure that
you are getting a file that starts with **jre-* and ends with **.tar.gz** (You must
also verify that you get the 32-bit or 64-bit version depending on which
Linux you’ve installed).

That site contains detailed instructions via help links.

Move the file to your home directory, then start a shell in your home
directory and run the command:

> tar zxvf jre-*.tar.gz

This creates a subdirectory starting with **jre** and ending with the version of
Java you just installed. Below is a **bin** directory. Edit your **.profile** (following
the instructions earlier in this atom) and locate the last **PATH** directive, if
there is one. Add or modify your **PATH** so Java’s **bin** directory is the first
one in your **PATH** (there are more “proper” ways to do this but we’re
being expedient). For example, the beginning of the **PATH** directive in your
~/.profile file can look like:

>export set PATH=/home/`whoami`/jre1.7.0_09/bin:$PATH: …

This way, if there are any other versions of Java on your system, the
version you just installed will always be seen first.

Reset your **PATH** with the command:
>source ~/.profile

(Or just close your shell and open a new one). Now you should be able to
run java -version and see a version number that agrees with what you’ve
just installed.

###Install Scala
The main download site for Scala is **www.scala-lang.org/downloads**. Scroll
through this page to locate the desired release number, and then
download the one marked “Unix, Mac OSX, Cygwin.” The file has an
extension of **.tgz**. After it downloads, move the file into your home
directory.

Start a shell in your home directory and run the command:
>tar zxvf scala-*.tgz

This creates a subdirectory starting with **scala-** and ending with the version
of Scala you just installed. Below is a **bin** directory. Edit your **.profile** file
and locate the **PATH** directive. Add the **bin** directory to your **PATH**, again
before the $PATH. For example, the **PATH** directive in your **~/.profile** file
can look like this:

>export set
>PATH=/home/`whoami`/jre1.7.0_09/bin:/home/`whoami`/scala-2.11.4/bin:$PATH:

Reset your **PATH** with the command
>source ~/.profile

(Or just close your shell and open a new one). Now you should be able to
run scala -version and see a version number that agrees with what you’ve
just installed.

!!!!!
###Source Code for the Book
We include a way to easily test the Scala exercises in this book with a
minimum of configuration and download. Follow the links for the book’s
source code at AtomicScala.com into a convenient location on your
computer.
Move atomic-scala-examples-master.zip to your home directory using the
shell command:
cp atomic-scala-examples-master.zip ~48 • Atomic Scala • Installation (Linux)
Unpack the book’s source code by running unzip atomic-scala-examplesmaster.zip. Navigate down into the resulting unpacked folder until you
find the examples directory.
Create an AtomicScala directory in your home directory, and move
examples into the AtomicScala directory. The ~/AtomicScala directory
now contains all the examples from the book in the subdirectory
examples.

###Set Your CLASSPATH
Note: Sometimes (on Linux, at least) you don’t need to set the CLASSPATH
at all and everything still works right. Before setting your CLASSPATH, try
running the testall.sh script (see below) and see if it’s successful.
The CLASSPATH is an environment variable used by Java (Scala runs atop
Java) to locate code files. If you want to place code files in a new directory,
then you must add that new directory to the CLASSPATH. For example, this
adds AtomicScala to your CLASSPATH when added to your ~/.profile,
assuming you installed into the AtomicScala subdirectory located off your
home directory:
export
CLASSPATH="/home/`whoami`/AtomicScala/examples:$CLASSPATH"Atomic Scala • Installation (Linux) • 49
The changes to CLASSPATH will take effect if you run:
source ~/.profile
or if you open a new shell.
Verify that everything is working by changing to the
AtomicScala/examples subdirectory. Then run:
scalac AtomicTest.scala
If everything is configured correctly, this creates a subdirectory
com/atomicscala that includes several files, including:
AtomicTest$.class
AtomicTest.class
Finally, test all the code in the book by running:
chmod +x testall.sh
./testall.sh
You get a couple of errors when you do this and that’s fine; it’s due to
things that we explain later in the book




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

To download **Coursier**, and set the **PATH**, we will be using `curl`. If your
computer already has `curl`, then you can skip to the next set of instructions.

>sudo apt install curl

Now use the following instructions to install **Coursier**:
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

!!!!!
To install several use full applications, including the SBT system, which will be very useful 
later, simply run the command:
> cs

This will trigger several downloads that may take a few minutes. 


#Introduction to Scala
## Running Scala
The Scala interpreter is also called the REPL (for Read-Evaluate-Print-Loop).
You get the REPL when you type **scala** by itself on the command line. You
should see something like the following (it can take a few moments to
start):

>Welcome to Scala version 2.11.4 (Java HotSpot(TM) 64-Bit <br />
>Server VM, Java 1.7.0_09). <br />
>Type in expressions to have them evaluated.<br />
>Type :help for more information.<br />
> 
> 
>scala> <br />
>
The exact version numbers will vary depending on the versions of Scala
and Java you’ve installed, but make sure that you’re running Scala 2.11 or
greater.

The REPL gives you immediate interactive feedback, which is helpful for
experimentation. For example, you can do arithmetic:

>scala> 42 * 11.3
>res0: Double = 474.6

**res0** is the name Scala gave to the result of the calculation. **Double** means
“double precision floating point number.” A floating-point number can

hold fractional values, and “double precision” refers to the number of
significant places to the right of the decimal point that the number is
capable of representing.

Find out more by typing **:help** at the Scala prompt. To exit the REPL, type:
>scala> :quit

##Comments
A *Comment* is illuminating text that is ignored by Scala. There are two
forms of comment. The // (two forward slashes) begins a comment that
goes to the end of the current line:

>47 * 42 // Single-line comment
>47 + 42

Scala will evaluate the multiplication, but will ignore the // and everything
after it until the end of the line. On the following line, it will pay attention
again and perform the sum.

The multiline comment begins with a /* (a forward slash followed by an
asterisk) and continues – including line breaks (which we call newlines) –
until a */ (an asterisk followed by a forward slash) ends the comment:

>47 + 42 /* A multiline comment
>Doesn't care
>about newlines */
> 
It’s possible to have code on the same line after the closing ***/** of a
comment, but it’s confusing so people don’t usually do it. In practice, you
see the **//** comment used a lot more than the multiline comment.

Comments should add new information that isn’t obvious from reading the
code. If the comments just repeat what the code says, it becomes annoying
(and people start ignoring your comments). When the code changes,
programmers often forget to update comments, so it’s a good practice to
use comments judiciously, mainly for highlighting tricky aspects of your
code.

##Scripting
A *script* is a file filled with Scala code that runs from the command-line
prompt. Suppose you have a file named **myfile.scala** containing a Scala
script. To execute that script from your operating system shell prompt,
enter:

>scala myfile.scala

Scala will then execute all the lines in your script. This is more convenient
than typing all those lines into the Scala REPL.

Scripting makes it easy to quickly create simple programs, so we use it
throughout much of this book (thus, you run the examples via `The Shell`).
Scripting solves basic problems, such as making utilities for your computer.
More sophisticated programs require the compiler, which we explore when
the time comes.

Using Sublime Text (from `Editors`), type in the following lines and save the
file as **ScriptDemo.scala**:

// ScriptDemo.scala
println("Hello, Scala!")

We always begin a code file with a comment that contains the name of the
file.

Assuming you’ve followed the instructions in the “Installation” section for
your computer’s operating system, the book’s examples are in a directory
called !!!!**AtomicScala**. Although you can download the code, we urge you to
type in the code from the book, since the hands-on experience can help
you learn.

The above script has a single executable line of code.
>println("Hello, Scala!")

When you run this script by typing (at the shell prompt):
>scala ScriptDemo.scala

You should see:
>Hello, Scala!

Now we’re ready to start learning about Scala.
##Values
A *value* holds a particular type of information. You define a value like this:
>val name = initialization

That is, the **val** keyword followed by the name (that you make up), an
equals sign and the initialization value. The name begins with a letter or an
underscore, followed by more letters, numbers and underscores. The dollar
sign ($) is for internal use, so don’t use it in names you make up. Upper
and lower case are distinguished (so **thisvalue** and **thisValue** are different).

Here are some value definitions:

> // Values.scala <br />
>
> val whole = 11 <br />
> val fractional = 1.4 <br />
> val words = "A value" <br />
>
> println(whole, fractional, words) <br />
>
> /* Output:<br />
> (11,1.4,A value)<br />
> */

The first line of each example in this book contains the name of the source
code file as you find it in the !!!!**AtomicScala** directory that you set up in your 
appropriate “Installation” atom. You also see line numbers on all of our
code samples. Line numbers do not appear in legal Scala code, so don’t
add them in your code. We use them merely as a convenience when
describing the code.

We also format the code in this book so it fits on an eBook reader page,
so we sometimes add line breaks – to shorten the lines – where they
would not otherwise be necessary.

On line 3, we create a value named **whole** and store 11 in it. Similarly, on
line 4, we store the “fractional number” 1.4, and on line 5 we store some
text (a string) in the value **words**.

Once you initialize a **val**, you can’t change it (it is constant or immutable).
Once we set **whole** to 11, for example, we can’t later say:

>whole = 15

If we do this, Scala complains, saying “error: reassignment to val.”

It’s important to choose descriptive names for your identifiers. This makes
your code easier to understand and often reduces the need for comments.
Looking at the code snippet above, you have no idea what whole
represents. If your program is storing the number 11 to represent the time 
of day when you get coffee, it’s more obvious to others if you name it
**coffeetime** and easier to read if it’s **coffeeTime**.

In the first few examples of this book, we show the output at the end of
the listing, inside a multiline comment. Note that **println** will take a single
value, or a comma-separated sequence of values.

We include exercises with each atom from this point forward. The solutions
are available at !!!!**AtomicScala.com**. The solution folders match the names of
the atoms.

# Compiling and Running Programs in IntelliJ IDEA

The easiest and fastest way to start using the examples in this book is by
compiling and running them using IntelliJ IDEA:

1. Follow the instructions [here](https://www.jetbrains.com/help/idea/installation-guide.html)
   to install IntelliJ IDEA.

!!!!!!
2. Download the [zipped code
   repository](https://github.com/BruceEckel/AtomicKotlinExamples/archive/master.zip)
   and [unzip it](#unpacking-a-zip-archive).

3. Start IntelliJ IDEA, and select the `File | Open` menu item.  Navigate to
   where you unzipped the repository and open the `build.sbt` file.
    
4. Go to the menu and select: <br />
   `View | Tool Windows | sbt`. Right-click the directory name, and reload the sbt project.
   This should ensure your machine has all necessary plugins and extensions installed.
   
5. In a terminal, run the command:
  > sbt

This will open the sbt shell. From there, you can run the programs in your project.

6. Here are several useful commands when using the sbt shell:

> compile

* This command will compile all the files in the project you are in.

>run

* This command will display a list of all executables in the project. In the comand prompt, input the 
index of the file to run.
  
>runMain (filename)

* This command will run the **Main** function in the file indicated.
* To run a program in a package, use the format **runMain packagename.mainName**


For example, to run `helloWorld`, the main function of the HelloWorld object located in
the directory `Examples`, input the following commands into the sbt shell:

>run <br />
> (input the index of helloWorld)
> 
or
> 
> runMain helloWorld

7. To exit the sbt shell, press **ctrl + d**


# Packages

If the program is in a package, the package name is also required to run the
program. That is, if `Foo.scala` contains a `package` statement:

```
package bar
```

then you cannot simply say:

```
scala Foo
```

You'll get a message starting with `error: could not find or load...`

If you were to compile this program, you'd see there's a new subdirectory
called `bar`. The name of the subdirectory that appears when you run `scalac`
corresponds to the `package` name in the program that was compiled.

```
scalac bar
```
If the program is packaged under `bar`, we give the package name followed by a
"dot," then the program's name:

```
scala bar.Foo
```

!!!!
# Appendix C: Testing

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