Spark Notebook
==============

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


*Fork of the amazing [scala-notebook](https://github.com/Bridgewater/scala-notebook), yet focusing on Massive Dataset Analysis using [Apache Spark](http://spark.apache.org).*

<!-- MarkdownTOC depth=4 autolink=true bracket=round -->

- [Description](#description)
- [Launch](#launch)
  - [Using a release](#using-a-release)
    - [ZIP](#zip)
    - [Docker](#docker)
    - [DEB](#deb)
  - [From the sources](#from-the-sources)
    - [Procedure](#procedure)
- [Use](#use)
- [Features](#features)
  - [Use/Reconfigure Spark](#usereconfigure-spark)
  - [Use the `form`](#use-the-form)
  - [The `reset` function](#the-reset-function)
  - [Keep an eye on your tasks](#keep-an-eye-on-your-tasks)
  - [Using (Spark)SQL](#using-sparksql)
    - [Static SQL](#static-sql)
    - [Dynamic SQL](#dynamic-sql)
    - [Show case](#show-case)
  - [Interacting with JavaScript](#interacting-with-javascript)
  - [Plotting with D3](#plotting-with-d3)
  - [Timeseries with  Rickshaw](#timeseries-with--rickshaw)
  - [Dynamic update of data and plot using Scala's `Future`](#dynamic-update-of-data-and-plot-using-scalas-future)
  - [Update _Notebook_ `ClassPath`](#update-_notebook_-classpath)
  - [Update __Spark__ dependencies (`spark.jars`)](#update-__spark__-dependencies-sparkjars)
- [IMPORTANT](#important)

<!-- /MarkdownTOC -->

Description
-----------
The main intent of this tool is to create [reproducible analysis](http://simplystatistics.org/2014/06/06/the-real-reason-reproducible-research-is-important/) using Scala, Apache Spark and more.

This is achieved through an interactive web-based editor that can combine Scala code, SQL queries, Markup or even JavaScript in a collaborative manner.

The usage of Spark comes out of the box, and is simply enabled by the implicit variable named `sparkContext`.

Launch
------
### Using a release

Long story short, there are several ways to start the spark notebook quickly (even from scratch):
 * ZIP file
 * Docker image
 * DEB package

However, there are several flavors for these distribtions that depends on the Spark version and Hadoop version you are using.

#### ZIP
The zip distributions are publicly available in the bucket: <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">s3://spark-notebook</a>.

**Checkout** the needed version <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">here</a>.

Here is an example how to use it:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/zip/spark-notebook-0.1.4-spark-1.2.0-hadoop-1.0.4.zip
unzip spark-notebook-0.1.4-spark-1.2.0-hadoop-1.0.4.zip
cd spark-notebook-0.1.4-spark-1.2.0-hadoop-1.0.4
./bin/spark-notebook
```

#### Docker
If you're a Docker user, the following procedure will be even simpler!

**Checkout** the needed version <a href="https://registry.hub.docker.com/u/andypetrella/spark-notebook/tags/manage/">here</a>.

```
docker pull andypetrella/spark-notebook:0.1.4-spark-1.2.0-hadoop-1.0.4
docker run -p 9000:9000 andypetrella/spark-notebook:0.1.4-spark-1.2.0-hadoop-1.0.4
```

#### DEB
Using debian packages is one of the standard, hence the spark notebook is also available in this form (from v0.1.4):


```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/deb/spark-notebook-0.1.4-spark-1.2.0-hadoop-1.0.4_all.deb
sudo dpkg -i spark-notebook-0.1.4-spark-1.2.0-hadoop-1.0.4.zip
sudo spark-notebook
```

**Checkout** the needed version <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">here</a>.


### From the sources
The spark notebook requires a [Java(TM)](http://en.wikipedia.org/wiki/Java_(programming_language)) environment (aka JVM) as runtime and [Play 2.2.6](https://www.playframework.com/documentation/2.2.6/Home) to build it.

Of course, you will also need a working [GIT](http://git-scm.com/) installation to download the code and build it.

#### Procedure
##### Download the code
```
git clone https://github.com/andypetrella/spark-notebook.git
cd spark-notebook
```
##### Launch the server
Enter the `play console` by running `play` within the `spark-notebook` folder:
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

To create your distribution
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
### Use/Reconfigure Spark
Since this project aims directly the usage of Spark, a [SparkContext](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkContext.scala) is added to the environment and can directly be used without additional effort.

![Example using Spark](https://raw.github.com/andypetrella/spark-notebook/master/images/simplest-spark.png)


Spark will start with a regular/basic configuration. There are different ways to customize the embedded Spark to your needs.

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


### Using (Spark)SQL
Spark comes with this handy and cool feature that we can write some SQL queries rather than boilerplating with
Scala or whatever code, with the clear advantage that the resulting DAG is optimized.

The spark-notebook offers SparkSQL support.
To access it, we first we need to register an `RDD` as a table:
```{scala}
dataRDD.registerTempTable("data")
```
Now, we can play with SQL in two different ways, the static and the dynamic ones.

#### Static SQL
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

#### Dynamic SQL
A dynamic SQL is looking like a static SQL but where specific tokens are used. Such tokens are taking the form: {`type`: `variableName`}.

When executing the command, the notebook will produce a form by generating on input for each dynamic part. See the show case below.

An example of such dynamic SQL is
```
:sql[selectKids] SELECT name FROM people WHERE name = "{String: name}" and age >= {Int: age}
```
Which will create a form with to inputs, one text and on number.

When changing the value in the inputs, the SQL is compiled on the server and the result is printed on the notebook (Success, Failure, Bad Plan, etc.).

Again, the result is completely reactive, hence using the `react` function is mandatory to use the underlying SchemaRDD (when it becomes valid!).


#### Show case
This is how it looks like in the notebook:

![Using SQL](https://raw.github.com/andypetrella/spark-notebook/master/images/reactive-spark-sql.png)


### Interacting with JavaScript
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


### Update _Notebook_ `ClassPath`
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

### Update __Spark__ dependencies (`spark.jars`)
So you use Spark, hence you know that it's not enough to have the jars locally added to the Driver's classpath.

Indeed, workers needs to have them in their classpath. One option would be to update the list of jars (`spark.jars` property) provided to the `SparkConf` using the `reset` function.

However, this can be very tricky when we need to add jars that have themselves plenty of dependencies.

However, there is another context available to update both the classpath on the notebook and in Spark

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
:dp org.bdgenomics.adam % adam-apis % 0.15.0
- org.apache.hadoop % hadoop-client %   _
- org.apache.spark  %     _         %   _
- org.scala-lang    %     _         %   _
- org.scoverage     %     _         %   _
```

In live, you can check the notebook named `Update classpath and Spark's jars`, which looks like this:

![Spark Jars](https://raw.github.com/andypetrella/spark-notebook/master/images/spark-jars.png)

## IMPORTANT
Some vizualizations (wisp) are currently using Highcharts which is **not** available for commercial or private usage!

If you're in this case, please to <a href="email:andy.petrella@gmail.com">contact</a> me first.
