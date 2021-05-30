#!/bin/bash
HOOK_NAMES="pre-commit pre-push"
# assuming the script is in a bin directory, one level into the repo
HOOK_DIR=$(git rev-parse --show-toplevel)/.git/hooks

echo $HOOK_DIR

for hook in $HOOK_NAMES; do
    # create the symlink, overwriting the file if it exists
    # probably the only way this would happen is if you're using an old version of git
    # -- back when the sample hooks were not executable, instead of being named ____.sample
    ln -f ./GitHooks/$hook $HOOK_DIR/$hook
done