Follow the directions for your OS here.

https://get-coursier.io/docs/cli-setup

When in doubt, open a new shell.
Downloads can take a long time and might appear to be frozen, just wait it out.

    eval "$(cs install --env)"

If Java is already installed, you might be missing the JDK, so execute this command to be sure

    cs java --jvm adopt:11 --setup

Periodically update your exectuables by re-installing them, eg:

    cs install scalafmt


## For contributors:

To ensure that you don't break the build and/or inconvenience your collaborators, execute this script in the project 
root to setup our hooks:

    ./bin/setupGitHooks.sh