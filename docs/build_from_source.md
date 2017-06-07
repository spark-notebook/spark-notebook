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
sbt -D"spark.version"="2.1.0" -D"hadoop.version"="2.7.3" -D"jets3t.version"="0.7.1" -Dmesos.version="0.26.0"
```

#### Create your distribution
For a simple `zip` distro, you can run
```
[spark-notebook] $ dist
```

In order to develop on the Spark Notebook, you'll have to use the `run` command instead.

### Customizing your build
If you want to change some build information like the `name` or the `organization` or even specific to `docker` for your builds.
Preferably you would want to avoid having to change the source code of this project.

You can do this by creating some hidden files being caught by the builder but kept out of git (added to `.gitignore`).

Here are the supported configuration.

#### Update main configuration
To update main configuration, `name` or `organization` alike, you can create a `.main.build.conf` next to `build.sbt`.

The structure of the file is the following:
```
main {
  name = "YourName"
  organization = "YourOrganization"
}
```

Check `project/MainProperties.scala` for details.

###### Update Docker configuration
Sometimes you may want to change the Docker image which is the result of the `sbt ... docker:publishLocal` command.

Create a file called `.docker.build.conf` next to `build.sbt`, this file is added to `.gitignore`.
The structure of the file is the following:
```
docker {
  maintainer = "Your maintainer details"
  registry = "your custom registry"
  baseImage = "your custom base image"
  commands = [
    { cmd = USER, arg = root },
    { cmd = RUN,  arg = "apt-get update -y && apt-get install -y ..." },
    { cmd = ENV,  arg = "MESOS_JAVA_NATIVE_LIBRARY /usr/lib/libmesos.so" },
    { cmd = ENV,  arg = "MESOS_LOG_DIR /var/log/mesos" }
  ]
}
```

Check `project/DockerProperties.scala` for details.

#### Using unreleased Spark version
While using the repository from SBT, you can use any version of Apache Spark you want (**up to 1.4 atm**), this will require several these things:

* you have a local install (maven) of the version (let's say Spark 1.4-RC1 or even Spark-1.4-SNAPSHOT)
* you run the sbt command using the wanted version `sbt -Dspark.version=1.4.0-SNAPSHOT`
* there is a folder in `modules/spark/src/main/scala-2.1X` that points to the base of the required version (excl. the classifier SNAPHOST or RC): `spark-1.6`
