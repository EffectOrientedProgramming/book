# Running the Book Code

## Install the Example Code

Download the [zipped code repository](https://github.com/EffectOrientedProgramming/examples).
To find the zipped version, press the green "Code" button.
Unpack the archive.

## Install IntelliJ IDEA

We use IntelliJ IDEA to develop the examples in this book.
You may use another environment such as VSCode but if you do you'll need to figure it out yourself.

1. Follow the instructions [here](https://www.jetbrains.com/help/idea/installation-guide.html)
   to install IntelliJ IDEA.

2. Start IntelliJ IDEA, and select the `File | Open` menu item.  Navigate to
   where you unzipped the repository and open the `build.sbt` file.

3. Go to the menu and select:
   `View | Tool Windows | sbt`. Right-click the directory name, and reload the sbt project.
   This should ensure your machine has all necessary plugins and extensions installed.

- If you get a message:
  ```
  Scalafmt configuration detected in this project
    Use scalafmt formatter
    Continue using IntelliJ formatter
  ```
  Select `Use scalafmt formatter`

- When you open a Scala file in IntelliJ, you might get a message in the title bar of that file:
  ```
  No Scala SDK in module
  ```
  On the right side of that message bar, you'll see:
  ```
  Setup Scala SDK
  ```
  Click on this. In the resulting dialog box, select "Create" and choose the latest Coursier version of Scala, which will be
  version 2.x. The Dialog box will now look something like this:

  ![image](https://user-images.githubusercontent.com/1001900/126879631-6490636e-7db5-4e4f-90c6-82292ff2569f.png)

  Select "OK". Now go to `File | Project Structure | Global Libraries` and Choose Scala 3, like this:

  ![image](https://user-images.githubusercontent.com/1001900/126879808-1285e65e-e674-4a9b-9246-c86f86956e90.png)
