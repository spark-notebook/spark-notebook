Spark Notebook
==============

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


*Fork of the amazing [scala-notebook](https://github.com/Bridgewater/scala-notebook), yet focusing on Massive Dataset Analysis using [Apache Spark](http://spark.apache.org).*

<!-- MarkdownTOC depth=5 autolink=true bracket=round -->

- [Description](#description)
  - [Discussions](#discussions)
  - [Mailing list](#mailing-list)
    - [Spark Notebook Dev](#spark-notebook-dev)
    - [Spark Notebook User](#spark-notebook-user)
- [Launch](#launch)
  - [Using a release](#using-a-release)
    - [Requirements](#requirements)
    - [ZIP](#zip)
    - [Docker](#docker)
    - [boot2docker (Mac OS X)](#boot2docker-mac-os-x)
    - [DEB](#deb)
  - [From the sources](#from-the-sources)
    - [Procedure](#procedure)
      - [Download the code](#download-the-code)
      - [Launch the server](#launch-the-server)
      - [Change relevant versions](#change-relevant-versions)
      - [Create your distribution](#create-your-distribution)
- [Use](#use)
- [Features](#features)
- [Configure Spark](#configure-spark)
  - [Preconfigure Spark](#preconfigure-spark)
    - [Set local repository](#set-local-repository)
    - [Add remote repositories](#add-remote-repositories)
    - [Import (download) dependencies](#import-download-dependencies)
    - [Default import statements](#default-import-statements)
    - [Spark Conf](#spark-conf)
    - [Example](#example)
      - [YARN](#yarn)
    - [Create a preconfigured notebook](#create-a-preconfigured-notebook)
      - [Update preconfigurations in metadata](#update-preconfigurations-in-metadata)
  - [Use the `form`](#use-the-form)
  - [The `reset` function](#the-reset-function)
  - [Keep an eye on your tasks](#keep-an-eye-on-your-tasks)
- [Using (Spark)SQL](#using-sparksql)
  - [Static SQL](#static-sql)
  - [Dynamic SQL](#dynamic-sql)
  - [Show case](#show-case)
- [Shell scripts `:sh`](#shell-scripts-sh)
- [Interacting with JavaScript](#interacting-with-javascript)
  - [Plotting with D3](#plotting-with-d3)
  - [WISP](#wisp)
  - [Timeseries with  Rickshaw](#timeseries-with--rickshaw)
  - [Dynamic update of data and plot using Scala's `Future`](#dynamic-update-of-data-and-plot-using-scalas-future)
- [Update _Notebook_ `ClassPath`](#update-_notebook_-classpath)
- [Update __Spark__ dependencies (`spark.jars`)](#update-__spark__-dependencies-sparkjars)
  - [Set `local-repo`](#set-local-repo)
  - [Add `remote-repo`](#add-remote-repo)
    - [`remote-repo` with authentication](#remote-repo-with-authentication)
  - [Download and add dependencies](#download-and-add-dependencies)
- [IMPORTANT](#important)
- [KNOWN ISSUES](#known-issues)
  - [`User limit of inotify watches reached`](#user-limit-of-inotify-watches-reached)

<!-- /MarkdownTOC -->

Description
-----------
The main intent of this tool is to create [reproducible analysis](http://simplystatistics.org/2014/06/06/the-real-reason-reproducible-research-is-important/) using Scala, Apache Spark and more.

This is achieved through an interactive web-based editor that can combine Scala code, SQL queries, Markup or even JavaScript in a collaborative manner.

The usage of Spark comes out of the box, and is simply enabled by the implicit variable named `sparkContext`.

### Discussions
C'mon on [gitter](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)!

### Mailing list
There are two different mailing lists, each aiming to specific discussions:

#### Spark Notebook Dev

Then [spark-notebook-dev](https://groups.google.com/forum/?hl=fr#!forum/spark-notebook-dev) mailing list for all threads regarding implementation, architecture, features and what not related to fix or enhance the project.

Email: [spark-notebook-dev@googlegroups.com](mailto:spark-notebook-dev@googlegroups.com).

#### Spark Notebook User

The [spark-notebook-user](https://groups.google.com/forum/?hl=fr#!forum/spark-notebook-user) is for almost everything else than dev, which are questions, bugs, complains, or hopefully some kindness :-D.

Email: [spark-notebook-user@googlegroups.com](mailto:spark-notebook-user@googlegroups.com).

Launch
------
### Using a release

Long story short, there are several ways to start the spark notebook quickly (even from scratch):
 * ZIP file
 * Docker image
 * DEB package

However, there are several flavors for these distributions that depends on the Spark version and Hadoop version you are using.

#### Requirements
* Make sure you're running at least Java 7 (`sudo apt-get install openjdk-7-jdk`).

#### ZIP
The zip distributions are publicly available in the bucket: <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">s3://spark-notebook</a>.

**Checkout** the needed version <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">here</a>.

Here is an example how to use it:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/zip/spark-notebook-0.2.0-spark-1.2.0-hadoop-1.0.4.zip
unzip spark-notebook-0.2.0-spark-1.2.0-hadoop-1.0.4.zip
cd spark-notebook-0.2.0-spark-1.2.0-hadoop-1.0.4
./bin/spark-notebook
```

#### Docker
If you're a Docker user, the following procedure will be even simpler!

**Checkout** the needed version <a href="https://registry.hub.docker.com/u/andypetrella/spark-notebook/tags/manage/">here</a>.

```
docker pull andypetrella/spark-notebook:0.2.0-spark-1.2.0-hadoop-1.0.4
docker run -p 9000:9000 andypetrella/spark-notebook:0.2.0-spark-1.2.0-hadoop-1.0.4
```

#### boot2docker (Mac OS X)
On Mac OS X, you need something like _boot2docker_ to use docker. However, port forwarding needs an extra command necessary for it to work (cf [this](http://stackoverflow.com/questions/28381903/spark-notebook-not-loading-with-docker) and [this](http://stackoverflow.com/questions/21653164/map-ports-so-you-can-access-docker-running-apps-from-osx-host) SO questions).

```
VBoxManage modifyvm "boot2docker-vm" --natpf1 "tcp-port9000,tcp,,9000,,9000"
```


#### DEB
Using debian packages is one of the standard, hence the spark notebook is also available in this form (from v0.2.0):


```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/deb/spark-notebook-0.2.0-spark-1.2.0-hadoop-1.0.4_all.deb
sudo dpkg -i spark-notebook-0.2.0-spark-1.2.0-hadoop-1.0.4.zip
sudo spark-notebook
```

**Checkout** the needed version <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">here</a>.


### From the sources
The spark notebook requires a [Java(TM)](http://en.wikipedia.org/wiki/Java_(programming_language)) environment (aka JVM) as runtime and [SBT](http://www.scala-sbt.org/) to build it.

Of course, you will also need a working [GIT](http://git-scm.com/) installation to download the code and build it.

#### Procedure
##### Download the code
```
git clone https://github.com/andypetrella/spark-notebook.git
cd spark-notebook
```

##### Launch the server
Enter the `sbt console` by running `sbt` within the `spark-notebook` folder:
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

##### Change relevant versions
When using **Spark** we generally have to take a lot of care with the **Spark** version itself but also the **Hadoop** version.
There is another dependency which is tricky to update, the **jets3t** one.

To update that, you can pass those version as properties, here is an example with the current default ones:
```
sbt -D"spark.version"="1.2.0" -D"hadoop.version"="1.0.4" -D"jets3t.version"="0.7.1"
```

##### Create your distribution
```
[spark-notebook] $ dist
```

In order to develop on the Spark Notebook, you'll have to use the `run` command instead.


Use
---
When the server has been started, you can head to the page `http://localhost:9000` and you'll see something similar to:
![Notebook list](https://raw.github.com/andypetrella/spark-notebook/master/images/list.png)

From there you can either:
 * create a new notebook or
 * launch an existing notebook

In both case, the `scala-notebook` will open a new tab with your notebook in it, loaded as a web page.

> Note: a notebook is a JSON file containing the layout and analysis blocks, and it's located
> within the project folder (with the `snb` extension).
> Hence, they can be shared and we can track their history in an SVM like `GIT`.

Features
--------

## Configure Spark
Since this project aims directly the usage of Spark, a [SparkContext](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkContext.scala) is added to the environment and can directly be used without additional effort.

![Example using Spark](https://raw.github.com/andypetrella/spark-notebook/master/images/simplest-spark.png)

By default, Spark will start with a regular/basic configuration. There are different ways to customize the embedded Spark to your needs.

### Preconfigure Spark
The cleanest way to configure Spark is actually to use the preconfiguration feature available in the **clusters** tab.

The basic idea is to configure a **template** that will act as a factory for notebooks:
![Clusters](https://raw.github.com/andypetrella/spark-notebook/master/images/clusters.png)

Then you'll have to fill some informations as Json, where you'll have to give a `name` and specify a `profile`. But specially:
 
#### Set local repository
When adding dependencies, it can be interesting to preconfigure a repository where some dependencies have been already fetched.

This will save the dependency manager to download the internet.

```json
    "customLocalRepo" : "/<home>/.m2/repository",
```

#### Add remote repositories

Some dependencies might not be available from usual repositories.

While the context `:remote-repo` is available from the notebook, we can also add them right in the preconfiguration:

```json
    "customRepos"     : [
      "s3-repo % default % s3://<bucket-name>/<path-to-repo> % (\"$AWS_ACCESS_KEY_ID\", \"$AWS_SECRET_ACCESS_KEY\")",
      "local % default % file://<home>/.m2/repository"
    ],
```

#### Import (download) dependencies

Adding dependencies in the classpath **and** in the spark context can be done, this way (see also `:dp`).

```json
    "customDeps"      : "med-at-scale        %  ga4gh-model-java % 0.1.0-SNAPSHOT\norg.apache.avro     %  avro-ipc         % 1.7.6\n- org.mortbay.jetty % org.eclipse.jetty % _",
```


#### Default import statements

Some package, classes, types, functions and so forth could be automatically imported, by using:

```json
    "customImports"   : "import scala.util.Random\n",
```

#### Spark Conf

Apache Spark needs some configuration to access clusters, tune the memory and [many others](http://spark.apache.org/docs/latest/configuration.html).

For this configuration to be shareable, and you don't want to use the `reset` functions, you can add:

```json
    "customSparkConf" : {
      "spark.app.name": "Notebook",
      "spark.master": "local[8]",
      "spark.executor.memory": "1G"
    }
```

#### Example
```json
{
  "name": "My cluster conf",
  "profile": "Local",
  "template": {
    "customLocalRepo" : "/<home>/.m2/repository",
    "customRepos"     : [
      "s3-repo % default % s3://<bucket-name>/<path-to-repo> % (\"$AWS_ACCESS_KEY_ID\", \"$AWS_SECRET_ACCESS_KEY\")",
      "local % default % file://<home>/.m2/repository"
    ],
    "customDeps"      : "med-at-scale        %  ga4gh-model-java % 0.1.0-SNAPSHOT\norg.apache.avro     %  avro-ipc         % 1.7.6\n- org.mortbay.jetty % org.eclipse.jetty % _",
    "customImports"   : "import scala.util.Random\n",
    "customSparkConf" : {
      "spark.app.name": "Notebook",
      "spark.master": "local[8]",
      "spark.executor.memory": "1G"
    }
  }
}
```

![Clusters](https://raw.github.com/andypetrella/spark-notebook/master/images/conf_cluster.png)

##### YARN

- Example YARN Cluster
```json
  "Example YARN" : {
    "name" : "Example YARN-Client",
    "profile" : "yarn-client",
    "status" : "stopped",
    "template" : {
      "customLocalRepo" : "",
      "customRepos" : [ ],
      "customDeps" : [ ],
      "customImports" : [ ],
      "customSparkConf" : {
        "spark.app.name" : "Notebook",
        "spark.master" : "yarn-client",
        "spark.executor.memory" : "1G",
        "spark.yarn.jar" : "hdfs:///user/spark/spark-assembly.jar"
      }
    }
  }
```

- Example YARN Profile
```json
  "yarn" : {
      "id" : "yarn-client",
      "name" : "YARN-Client",
      "template" : {
        "customLocalRepo" : null,
        "customRepos" : null,
        "customDeps" : null,
        "customImports" : null,
        "customSparkConf" : {
          "spark.app.name" : "Notebook",
          "spark.master" : "yarn-client",
          "spark.executor.memory" : "1G",
          "spark.yarn.jar" : "hdfs:///user/spark/spark-assembly.jar"
        }
      }
   }
```

To using YARN cluster,

1. Put the spark-assembly-*.jar to the HDFS
```
# sudo -u hdfs hdfs dfs -mkdir -p /user/spark
# sudo -u hdfs hdfs dfs -put /usr/lib/spark/lib/spark-assembly.jar /user/spark/spark-assembly.jar
```
1. Point the location of spark-assembly.jar with `spark.yarn.jar` property.
1. Add Hadoop Conf dir such as `/etc/hadoop/conf` to the classpath in the executable script `bin/spark-notebook`:
```
declare -r script_conf_file="/etc/default/spark-notebook"

declare -r app_classpath="/etc/hadoop/conf:$lib_dir/...

addJava "-Duser.dir=$(cd "${app_home}/.."; pwd -P)"
```
1. Start spark-notebook, then create notebook from example yarn cluster. After a while, spark should be initialized and `sparkContext` will be ready to use.

#### Create a preconfigured notebook
Now you can use the configuration, by clicking `create`
![Clusters](https://raw.github.com/andypetrella/spark-notebook/master/images/create_pre_notebook.png)

You'll have the hand on the configuration (fine tuning) before creating the notebook:
![Clusters](https://raw.github.com/andypetrella/spark-notebook/master/images/conf_new_notebook.png)

##### Update preconfigurations in metadata
Actually, the configuration are stored in the `metadata` of the notebooks.

To change them, simply go to `edit > Edit Notebook Metadata` and you'll have:
![Clusters](https://raw.github.com/andypetrella/spark-notebook/master/images/edit_conf_notebook.png)


### Use the `form`
In order to adapt the configuration of the `SparkContext`, one can add the widget `notebook.front.widgets.Spark`.
This widget takes the current context as only argument and will produce an HTML `form` that will allow manual and friendly changes to be applied.

So first, adding the widget in a cell,
```{scala}
import notebook.front.widgets.Spark
new Spark(sparkContext)
```

Run the cell and you'll get,
![Using SQL](https://raw.github.com/andypetrella/spark-notebook/master/images/update-spark-conf-form.png)

It has two parts:
 * the first one is showing an input for each current properties
 * the second will add new entries in the configuration based on the provided name

Submit the first part and the `SparkContext` will restart in the background (you can check the Spark UI to check if you like).


### The `reset` function
The  *function* `reset` is available in all notebooks: This function takes several parameters, but the most important one is `lastChanges` which is itself a function that can adapt the [SparkConf](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkConf.scala). This way, we can change the *master*, the *executor memory* and a *cassandra sink* or whatever before restarting it. For more Spark configuration options see: [Spark Configuration]([Spark Configuration](https://spark.apache.org/docs/1.1.0/configuration.html##available-properties))

In this example we reset `SparkContext` and add configuration options to use the [cassandra-connector]:
```{scala}
import org.apache.spark.{Logging, SparkConf}
val cassandraHost:String = "localhost"
reset(lastChanges= _.set("spark.cassandra.connection.host", cassandraHost))
```
This makes Cassandra connector avaible in the Spark Context. Then you can use it, like so:
```{scala}
import com.datastax.spark.connector._
sparkContext.cassandraTable("test_keyspace", "test_column_family")
```

### Keep an eye on your tasks
Accessing the Spark UI is not always allowed or easy, hence a simple widget is available for us to keep a little eye on the stages running on the Spark cluster.

Luckily, it's fairly easy, just add this to the notebook:
```{scala}
import org.apache.spark.ui.notebook.front.widgets.SparkInfo
import scala.concurrent.duration._
new SparkInfo(sparkContext, checkInterval=1 second, execNumber=Some(100))
```
This call will show and update a feedback panel tracking some basic (atm) metrics, in this configuration there will be **one check per second**, but will check only **100 times**.

This can be tuned at will, for instance for an infinte checking, one can pass the `None` value to the argument `execNumber`.

Counting the words of a [wikipedia dump](http://en.wikipedia.org/wiki/Wikipedia:Database_download) will result in
![Showing progress](https://raw.github.com/andypetrella/spark-notebook/master/images/spark-tracker.png)


## Using (Spark)SQL
Spark comes with this handy and cool feature that we can write some SQL queries rather than boilerplating with
Scala or whatever code, with the clear advantage that the resulting DAG is optimized.

The spark-notebook offers SparkSQL support.
To access it, we first we need to register an `RDD` as a table:
```{scala}
dataRDD.registerTempTable("data")
```
Now, we can play with SQL in two different ways, the static and the dynamic ones.

### Static SQL
Then we can play with this `data` table like so:
```
:sql select col1 from data where col2 == 'thingy'
```
This will give access to the result via the `resXYZ` variable.

This is already helpful, but the `resXYZ` nummering can change and is not friendly, so we can also give a name to the result:
```
:sql[col1Var] select col1 from data where col2 == 'thingy'
```
Now, we can use the variable `col1Var` wrapping a `SchemaRDD`.

This variable is reactive meaning that it react to the change of the SQL result. Hence in order to deal with the result, you can access its `react` function which takes two arguments:
 * a **function** to apply on the underlying `SchemaRDD` to compute a result
 * a **widget** that will take the result of the function applied to the `SchemaRDD` and use it to update its rendering

The power of this reactivity is increased when we use SQL with dynamic parts.

### Dynamic SQL
A dynamic SQL is looking like a static SQL but where specific tokens are used. Such tokens are taking the form: {`type`: `variableName`}.

When executing the command, the notebook will produce a form by generating on input for each dynamic part. See the show case below.

An example of such dynamic SQL is
```
:sql[selectKids] SELECT name FROM people WHERE name = "{String: name}" and age >= {Int: age}
```
Which will create a form with to inputs, one text and on number.

When changing the value in the inputs, the SQL is compiled on the server and the result is printed on the notebook (Success, Failure, Bad Plan, etc.).

Again, the result is completely reactive, hence using the `react` function is mandatory to use the underlying SchemaRDD (when it becomes valid!).


### Show case
This is how it looks like in the notebook:

![Using SQL](https://raw.github.com/andypetrella/spark-notebook/master/images/reactive-spark-sql.png)


## Shell scripts `:sh`
There is a way to easily use (rudimentary) shell scripts via the `:sh` context.

```sh
:sh ls -la ~/data
```

## Interacting with JavaScript
Showing numbers can be good but great analysis reports should include relevant charts, for that we need JavaScript to manipulate the notebook's DOM.

For that purpose, a notebook can use the `Playground` abstraction. It allows us to create data in Scala and use it in predefined JavaScript functions (located under `assets/javascripts/notebook`) or even JavaScript snippets (that is, written straight in the notebook as a Scala `String` to be sent to the JavaScript interpreter).

The JavaScript function will be called with these parameters:
 * the data observable: a JS function can register its new data via `subscribe`.
 * the dom element: so that it can update it with custom behavior
 * an extra object: any additional data, configuration or whatever that comes from the Scala side

Here is how this can be used, with a predefined `consoleDir` JS function ([see here](https://github.com/andypetrella/spark-notebook/blob/master/observable/src/main/assets/observable/js/consoleDir.coffee)):

![Simple Playground](https://raw.github.com/andypetrella/spark-notebook/master/images/simple-pg.png)

Another example using the same predefined function and example to react on the new incoming data (more in further section). The __new stuff__ here is the use of `Codec` to convert a Scala object into the JSON format used in JS:

![Playground with custom data](https://raw.github.com/andypetrella/spark-notebook/master/images/data-pg.png)


### Plotting with [D3](http://d3js.org/)
Plotting with D3.js is rather common now, however it's not always simple, hence there is a Scala wrapper that brings the bootstrap of D3 in the mix.

These wrappers are `D3.svg` and `D3.linePlot`, and they are just a proof of concept for now. The idea is to bring Scala data to D3.js then create `Coffeescript` to interact with them.

For instance, `linePlot` is used like so:

![Using Rickshaw](https://raw.github.com/andypetrella/spark-notebook/master/images/linePlot.png)


> Note: This is subject to future change because it would be better to use `playground` for this purpose.

### [WISP](https://github.com/quantifind/wisp)
WISP is really easy to use to draw plots, however a simple wrapper has been created to ease the integration.

First import the helper:
```scala
import notebook.front.third.wisp._
```

Here is how you can draw an area chart:
```scala
Plot(Seq(SummarySeries((0 to 9) zip (10 to 100 by 10), "area")))
```

And a bar plot with category values
```scala
import com.quantifind.charts.highcharts.Axis
Plot(Seq(SummarySeries((0 to 9) zip (10 to 100 by 10), "column")),
    xCat = Some(Seq("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"))
)
```

### Timeseries with  [Rickshaw](http://code.shutterstock.com/rickshaw/)
Plotting timeseries is very common, for this purpose the spark notebook includes Rickshaw that quickly enables handsome timeline charts.

Rickshaw is available through `Playground` and a dedicated function for simple needs `rickshawts`.

To use it, you are only required to convert/wrap your data points into a dedicated `Series` object:
```{scala}
def createTss(start:Long, step:Int=60*1000, nb:Int = 100):Seq[Series] = ...
val data = createTss(orig, step, nb)

val p = new Playground(data, List(Script("rickshawts",
                                          Json.obj(
                                            "renderer" → "stack",
                                            "fixed" → Json.obj(
                                                        "interval" → (step/1000),
                                                        "max" → 100,
                                                        "baseInSec" → (orig/1000)
                                                      )
                                          ))))(seriesCodec)
```
As you can see, the only big deal is to create the timeseries (`Seq[Series]` which is a simple wrapper around:
 * name
 * color
 * data (a sequence of `x` and `y`)

Also, there are some options to tune the display:
 * provide the type of renderer (`line`, `stack`, ...)
 * if the timeseries will be updated you can fix the window by supplying the `fixed` object:
  * interval (at which data is upated)
  * max (the max number of points displayed)
  * the unit in the `X` axis.

Here is an example of the kind of result you can expect:

![Using Rickshaw](https://raw.github.com/andypetrella/spark-notebook/master/images/use-rickshaw.png)


### Dynamic update of data and plot using Scala's `Future`
One of the very cool things that is used in the original `scala-notebook` is the use of reactive libs on both sides: server and client, combined with WebSockets. This offers a neat way to show dynamic activities like streaming data and so on.

We can exploit the reactive support to update Plot wrappers (the `Playground` instance actually) in a dynamic manner. If the JS functions are listening to the data changes they can automatically update their result.

The following example is showing how a timeseries plotted with Rickshaw can be regularly updated. We are using Scala `Futures` to simulate a server side process that would poll for a third-party service:

![Update Timeseries Result](https://raw.github.com/andypetrella/spark-notebook/master/images/dyn-ts-code.png)

The results will be:

![Update Timeseries Result](https://raw.github.com/andypetrella/spark-notebook/master/images/dyn-ts.gif)


## Update _Notebook_ `ClassPath`
Keeping your notebook runtime updated with the libraries you need in the classpath is usually cumbersome as it requires updating the server configuration in the SBT definition and restarting the system. Which is pretty sad because it requires a restart, rebuild and is not contextual to the notebook!

Hence, a dedicated context has been added to the block, `:cp` which allows us to add specifiy __local paths__ to jars that will be part of the classpath.

```
:cp /home/noootsab/.m2/repository/joda-time/joda-time/2.4/joda-time-2.4.jar
```
Or even
```
:cp
/tmp/scala-notebook/repo/com/codahale/metrics/metrics-core/3.0.2/metrics-core-3.0.2.jar
/tmp/scala-notebook/repo/org/scala-lang/scala-compiler/2.10.4/scala-compiler-2.10.4.jar
/tmp/scala-notebook/repo/org/scala-lang/scala-library/2.10.4/scala-library-2.10.4.jar
/tmp/scala-notebook/repo/joda-time/joda-time/2.3/joda-time-2.3.jar
/tmp/scala-notebook/repo/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar
/tmp/scala-notebook/repo/com/datastax/cassandra/cassandra-driver-core/2.0.4/cassandra-driver-core-2.0.4.jar
/tmp/scala-notebook/repo/org/apache/thrift/libthrift/0.9.1/libthrift-0.9.1.jar
/tmp/scala-notebook/repo/org/apache/httpcomponents/httpcore/4.2.4/httpcore-4.2.4.jar
/tmp/scala-notebook/repo/org/joda/joda-convert/1.2/joda-convert-1.2.jar
/tmp/scala-notebook/repo/org/scala-lang/scala-reflect/2.10.4/scala-reflect-2.10.4.jar
/tmp/scala-notebook/repo/org/apache/cassandra/cassandra-clientutil/2.0.9/cassandra-clientutil-2.0.9.jar
/tmp/scala-notebook/repo/org/slf4j/slf4j-api/1.7.2/slf4j-api-1.7.2.jar
/tmp/scala-notebook/repo/com/datastax/cassandra/cassandra-driver-core/2.0.4/cassandra-driver-core-2.0.4-sources.jar
/tmp/scala-notebook/repo/io/netty/netty/3.9.0.Final/netty-3.9.0.Final.jar
/tmp/scala-notebook/repo/org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar
/tmp/scala-notebook/repo/commons-codec/commons-codec/1.6/commons-codec-1.6.jar
/tmp/scala-notebook/repo/org/apache/httpcomponents/httpclient/4.2.5/httpclient-4.2.5.jar
/tmp/scala-notebook/repo/org/apache/cassandra/cassandra-thrift/2.0.9/cassandra-thrift-2.0.9.jar
/tmp/scala-notebook/repo/com/datastax/spark/spark-cassandra-connector_2.10/1.1.0-alpha1/spark-cassandra-connector_2.10-1.1.0-alpha1.jar
/tmp/scala-notebook/repo/com/google/guava/guava/15.0/guava-15.0.jar
```

Here is what it'll look like in the notebook:

![Simple Classpath](https://raw.github.com/andypetrella/spark-notebook/master/images/simple-cp.png)

## Update __Spark__ dependencies (`spark.jars`)
So you use Spark, hence you know that it's not enough to have the jars locally added to the Driver's classpath.

Indeed, workers needs to have them in their classpath. One option would be to update the list of jars (`spark.jars` property) provided to the `SparkConf` using the `reset` function.

However, this can be very tricky when we need to add jars that have themselves plenty of dependencies.

Thus, there is another context available to update both the classpath on the notebook and in Spark. Before introducing it, we need first to introduce two other concepts.

### Set `local-repo`
When updating you dependencies, you can either leave the system create a repo (temporary) for you where it'll fetch the dependencies.

Or, you can set it yourself by using the `:local-repo` this way:
```
:local-repo /path/to/repo
```

This way, you can reuse local dependencies or reuse pre-downloaded ones.

### Add `remote-repo`

To instruct the system where to look for dependencies, you'll have to use the `:remote-repo` context:
```
:remote-repo oss-sonatype % default % https://oss.sonatype.org/content/repositories/releases/
```

Above we defined a repo named `oss-sonatype` with `default` structure at localtion `https://oss.sonatype.org/content/repositories/releases/`.

#### `remote-repo` with authentication

Some repos (on S3 for instance) require authentication, for this you can add them **literally** or using **env variables**:

```
:remote-repo :remote-repo s3-repo % default % s3://<bucket-name>/<path-to-repo> % ("$AWS_ACCESS_KEY_ID", "$AWS_SECRET_ACCESS_KEY")
```



### Download and add dependencies
So to add dependencies based on the context that has been set using the above contexts can be done using `:dp`.

```
:dp
+ group1 % artifact1 % version1
+ group2 % artifact2 % version2
group3 % artifact3 % version3
+ group4 % artifact4 % version4

- group5 % artifact5 % version5
+ group6 % artifact6 % version6
- group7 % artifact7 % version7
```

So this is simple:
* lines starting with `-` are exclusions (transitive)
* lines starting with `+` or nothing are inclusions

The jars will be fetched in a temporary repository (that can be hardcoded using `:local-repo`).

Then they'll be added to the Spark's jars property, before restarting the context.


For example, if you want to use [ADAM](https://github.com/bigdatagenomics/adam), all you need to do is:
```
:dp org.bdgenomics.adam % adam-apis % 0.16.0
- org.apache.hadoop % hadoop-client %   _
- org.apache.spark  %     _         %   _
- org.scala-lang    %     _         %   _
- org.scoverage     %     _         %   _
```

In live, you can check the notebook named `Update classpath and Spark's jars`, which looks like this:

![Spark Jars](https://raw.github.com/andypetrella/spark-notebook/master/images/spark-jars.png)

## TIPS AND TROUBLESHOOTING

There are some common problems that users experience from time to time.
So we collected some useful tips to make your life easier:

* spark-notebook uses old hadoop 1.0.4 by default. As notebook is a spark-driver itself, hence it defines the dependencies to be used within the cluster. This means that the `hadoop-client` has to match the cluster one, that's why we need to start the correct hadoop version in (or download the right distro),
you should start spark-notebook with -Dhadoop.version parameter, like:
```
sbt -Dhadoop.version=2.4.0 run
```
* many errors are not yet reported directly to notebook console. So, if something is wrong do not forget to look at **logs/sn-session.log** and at spark worker's logs.
* your current spark configuration is shown in **Edit > Edit Notebook Metadata**. You can make changes there instead of adding a special cell for `reset`ing default spark configuration. You can also create a template for spark configuration in a "Clusters" tab.
* some features (like switching output modes of the cell) are activated by keyboard shortcuts that are described at **Help > Keyboard Shortcuts**.


## IMPORTANT
Some vizualizations (wisp) are currently using Highcharts which is **not** available for commercial or private usage!

If you're in this case, please to <a href="email:andy.petrella@gmail.com">contact</a> me first.


## KNOWN ISSUES

### `User limit of inotify watches reached`

When running Spark-Notebook on some Linux distribs (specifically ArchLinux), you may encounter this exception:

```
[spark-notebook] $ run
 
java.io.IOException: User limit of inotify watches reached
at sun.nio.fs.LinuxWatchService$Poller.implRegister(LinuxWatchService.java:261)
at sun.nio.fs.AbstractPoller.processRequests(AbstractPoller.java:260)
at sun.nio.fs.LinuxWatchService$Poller.run(LinuxWatchService.java:326)
at java.lang.Thread.run(Thread.java:745)
[trace] Stack trace suppressed: run last sparkNotebook/compile:run for the full output.
[error] (sparkNotebook/compile:run) java.lang.reflect.InvocationTargetException
[error] Total time: 1 s, completed Jan 31, 2015 7:21:58 PM 
```

This certainly means your `sysctl` configuration limits too much `inotify` watches.

You must increase the parameter `fs.inotify.max_user_watches`.

To get current value:

```
$ sudo sysctl -a | grep fs.inotify.max_user_watches
fs.inotify.max_user_watches = 8192
```

To increase this value, create a new file `/etc/sysctl.d99-sysctl.conf`

```
fs.inotify.max_user_watches=100000
```

Refresh your live `sysctl` configuration:

```
$ sudo sysctl --system
```
