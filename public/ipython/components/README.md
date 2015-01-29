# IPython javascript components

Some installed with Bower, others installed with Git, because bower can't install everything.
This is for use as a submodule in IPython.

This requires bower, which can be installed with

    npm install -g bower


## Updating components

To update this repo, make any appropriate changes to `bower.json`,
and run

    bower install

This scrubs the components, and does a fresh bower install of everything,
and any post-processing steps described in the `fabfile`.

The only files that should ever be edited by humans are:

    bower.json
    .gitignore

This repo should never contain any manual changes to any of the components.
