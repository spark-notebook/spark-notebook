Scala Notebook
==============

A more friendly, browser-based interactive Scala prompt (REPL).

Based on the IPython notebook project, this project will let you interact with Scala in a browser window, which has the following advantages over the standard REPL:

* Easy to view and edit past commands
* Commands can return HTML or images, allowing richer interactivity (charts, for example)
* Notebooks can be saved and loaded, providing a bridge between interactive REPL and classes in a project
* Supports mixing Scala expressions and markdown, letting you create rich, interactive documents similar to Mathematica

While I think this tool will be helpful for everyone using Scala, I expect it to be particularly valuable for the scientific and analytics community.


Using Scala Notebook
----------------------

## Building From Source
* To build and run from SBT, type

```scala
project server
run
```

![Alt text](http://i.imgur.com/8wnrP34.png)

Development
-----------

[![Build Status](https://secure.travis-ci.org/Bridgewater/scala-notebook.png?branch=master)](http://travis-ci.org/Bridgewater/scala-notebook)

### IDE Setup

* If you're using an IntelliJ project, note that by default IntelliJ will not include SSP files resources. Change settings in IntelliJ to to include '*' as resource extension.

### Overview

Having the web server process separate from the process doing the evaluation is also important in Scala; we want to separate
the user's actions from the web server, allowing a restart of the client process (after building new client libraries, for example).

To that end, the project is organized as follows:
* *server* is the web server
* *common* are the classes shared by both
* *observable* 
* *kernel*
* *subprocess*


### Architecture

* Server
* Kernel(s)
* Widgets
