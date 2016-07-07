# Documentation

## Building from the sources
The Spark Notebook requires a [Java(TM)](http://en.wikipedia.org/wiki/Java_(programming_language)) environment (aka JVM) as runtime and [SBT](http://www.scala-sbt.org/) to build it.

Of course, you will also need a working [GIT](http://git-scm.com/) installation to download the code and build it.

### Procedure
#### Download the code
```
git clone https://github.com/andypetrella/spark-notebook.git
cd spark-notebook
```

#### Launch the server
Execute `sbt` within the `spark-notebook` folder:
```
[info] Loading global plugins from /home/noootsab/.sbt/0.13/plugins
[info] Loading project definition from /home/Sources/noootsab/spark-notebook/project
[info] Set current project to spark-notebook (in build file:/home/Sources/noootsab/spark-notebook/)
       _
 _ __ | | __ _ _  _
| '_ \| |/ _' | || |
|  __/|_|\____|\__ /
|_|            |__/

play 2.2.6 built with Scala 2.10.3 (running Java 1.7.0_72), http://www.playframework.com

> Type "help play" or "license" for more information.
> Type "exit" or use Ctrl+D to leave this console.

[spark-notebook] $
```
Then you will need to execute the `run` command that will launch the server. In `run` mode, your changes in the code will be compiled behind the scene and will reload the application for you.


#### Change relevant versions
When using **Spark** we generally have to take a lot of care with the **Spark** version itself but also the **Hadoop** version.
There is another dependency which is tricky to update, the **jets3t** one.

To update that, you can pass those version as properties, here is an example with the current default ones:
```
sbt -D"spark.version"="1.5.0" -D"hadoop.version"="2.6.0" -D"jets3t.version"="0.7.1" -Dmesos.version="0.24.0"
```

#### Create your distribution
For a simple `zip` distro, you can run
```
[spark-notebook] $ dist
```

In order to develop on the Spark Notebook, you'll have to use the `run` command instead.