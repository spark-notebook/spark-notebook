# sbt-project-generation

This is a library which can generate an SBT project from a [Spark Notebook](https://github.com/andypetrella/spark-notebook/).

The code is still experimental.

# How to enable it?

Uncomment and adapt the mandatory `sbt-project-generation` section in `conf/application.conf` file. 

```conf
sbt-project-generation {
  projects.dir = "/tmp/generated-sbt-projects" # dir where to store the generated sbt projects
  dependencies {
    # where to find jdk and sbt (used for compilation)
    jdk_home = "/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home",
    sbt_home = "/usr/local"
  }
}
```

You may also optionally:
- set up the generated code to be uploaded to a Git repo (beware it does a force push each time!).
  * [More details on setting up Git auth here](/modules/git-notebook-provider/README.md)
- set up the artifactory to automatically publish to


# What does it do?
It generates a project in `/tmp/generated-projects-dir/a-notebook-name` composed of

* `build.sbt`
* `src/scala/App.scala`
* ...

Now you can check if it really compiles:
```
sbt compile
```
P.S. you may need to take care or small differences between Scala REPL and compiled code, e.g.
do not define same name multiple times



# What you can do with the generated code ?

```bash
# Go to a folder with generated code
cd /tmp/generated-projects-dir/a-notebook-name
sbt run
# or also:
sbt package # create a single uberjar including all dependencies JARs
sbt packageBin  # create a .zip archive
sbt debian:packageBin  # for creating a .deb
sbt publish # publish to a Maven repository

# or even publish to docker:
sbt docker:publishLocal
sbt docker:publish some-repo/project-name:some-version
```

This will compile and run the project, which creates the spark context and runs the code included in the notebook.
