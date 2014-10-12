Spark Notebook
==============
*Fork of the amazing [scala-notebook](https://github.com/Bridgewater/scala-notebook), yet focusing on Massive Dataset Analysis ugin [Apache Spark](http://spark.apache.org).*

Description
-----------
The main intent of this tool is to create [reproductible analysis](http://simplystatistics.org/2014/06/06/the-real-reason-reproducible-research-is-important/) using Scala, Apache Spark and more.

The interaction is only made through a web page that can combine Scala code, SQL queries, Markup or even JavaScript in a collaborative manner.

The usage of Spark comes out of the box, and is simply enabled by the implicit variable named `sparkContext`.

Launch
------
## Very quickstart

Long story short, there is a small script that can help you setup and launch it without any other requirements than a _java environment_.


```bash
curl https://raw.githubusercontent.com/andypetrella/spark-notebook/master/run.sh | bash
```

## Longer story
The spark notebook is relying on several tools to work:
 * [JAVA](http://en.wikipedia.org/wiki/Java_(programming_language))
 * [SBT](www.scala-sbt.org)

It needs to be "installed", which means downloading the code and build it, this will need extra tools:
 * [GIT](http://git-scm.com/)

### Procedure
#### Download the code
```
git clone https://github.com/andypetrella/spark-notebook.git
cd spark-notebook
```
#### Launch the server
Enter the `sbt` console by running within the `spark-notebook`:
```
spark-notebook$ sbt
[warn] Multiple resolvers having different access mechanism configured with same name 'sbt-plugin-releases'. To avoid conflict, Remove duplicate project resolvers (`resolvers`) or rename publishing resolver (`publishTo`).
[info] Updating {file:/home/noootsab/src/noootsab/spark-notebook/project/}spark-notebook-build...
...
...
...
```

Then you can head to the `server` and run it, you have several options available:
 * `--disable_security`: this will disable the Akka secured cookie (helpful in local env)
 * `--no_browser`: this prevents the project to open a page in your browser everytime the server is started

```
> project server
> run --disable_security
```

Use
---
When the server has been started, you can head to the page `http://localhost:8899` and you'll see something similar to:
![Notebook list](https://raw.github.com/andypetrella/spark-notebook/spark/images/list.png)

From there you can either:
 * create a new notebook
 * launch an existing notebook
 
In both case, the `scala-notebook` will open a new tab with our notebook in it, that is a web page.

> Note: a notebook is nothing other than a JSON file containing the layout and analysis blocks, and it's located 
> within the project folder (with the `snb` extension).
> Hence, they can be shared and we can track their history in an SVM like `GIT`.

Features
--------
## Use/Reconfigure Spark
Since this project aims directly the usage of Spark, a [SparkContext](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkContext.scala) is added to the environment and can directly be used without much effort.

![Example using Spark](https://raw.github.com/andypetrella/spark-notebook/spark/images/simplest-spark.png)


However, it will start with a regular/basic configuration, hence a *function* is also provided in order to adapt it, `reset`. This function takes several parameters, but the most important one is `lastChanges` which is itself a function that can adapt the [SparkConf](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkConf.scala). Hence, we can change the *master*, the *executor memory* and a *cassandra sink* or whatever before restarting it.

Here is how to reset the `SparkContext` with the [cassandra-connector] configuration in the mix:
```{scala}
import org.apache.spark.{Logging, SparkConf}
import com.datastax.spark.connector._
val cassandraHost:String = "localhost" 
reset(lastChanges= _.set("spark.cassandra.connection.host", cassandraHost))
```
Then you can use it, like so:
```{scala}
sparkContext.cassandraTable("test_ks", "test_cf")

```
## Using (Spark)SQL
Spark comes with this handy and cool feature that we can write some SQL queries rather than boilerplating with 
Scala or whatever code, with the clear advantage that the resulting DAG is optimize.

Hence, this support also has been added to the notebook, this is as simple as the following. First we need to register an `RDD` as a table:
```{scala}
dataRDD.registerTempTable("data")
```

Then we can play with this `data` table like so:
```
:sql select col1 from data where col2 == 'thingy'
```
This will give access to the result via the `resX` variable.

This is already helpful, but the `redX` can change and is not friendly, so we can also name the result likewise:
```
:sql[col1Var] select col1 from data where col2 == 'thingy'
```
Now, we can use the variable `col1Var` which is a RDD that we can manipulate further.

This is how it looks like in the notebook:

![Using SQL](https://raw.github.com/andypetrella/spark-notebook/spark/images/sql.png)


## Interacting with JavaScript
Showing numbers can be good but great analysis reports must include relevant charts, for that we need javascript to manipulate the DOM.

For that purpose, a notebook can make usage of the `Playground` abstraction. It allows us to create data in Scala and use it in predefined JavaScript functions (located under `observable/src/main/assets/observable/js`) or even JavaScript snippets (that is, written straight in the notebook as a Scala `String`).

The JavaScript function will be called with these parameters:
 * the data observable: a JS function can register its new data via `subscribe`.
 * the dom element: so that it can update it with custom behavior
 * an extra object: a configuration or whatever but that comes from the Scala world

Here is how this can be used, with a predefined `consoleDir` JS function ([see here](https://github.com/andypetrella/spark-notebook/blob/spark/observable/src/main/assets/observable/js/consoleDir.coffee)):

![Simple Playground](https://raw.github.com/andypetrella/spark-notebook/spark/images/simple-pg.png)

Another example using the same predefined function and example to react on the new incoming data (more in further section). The __new stuff__ here is the use of `Codec` to convert a Scala object into the JSON format used in JS:

![Playground with custom data](https://raw.github.com/andypetrella/spark-notebook/spark/images/data-pg.png)


## Plotting with [D3](http://d3js.org/)
Plotting with D3.js is rather common now, however it's not always simple, hence there is a Scala wrappers that brings the boostrap of it in the mix.

These wrappers are `D3.svg` and `D3.linePlot`, and there just proof of concept for now. Actually the idea is to bring Scala data to D3.js then create `Coffeescript` to interact with them.

For instance, `linePlot` is used like so:

![Using Rickshaw](https://raw.github.com/andypetrella/spark-notebook/spark/images/linePlot.png)


> Note: This is subject to refactorings because it would be better to use `playground` for this purpose.

## Timeseries with  [Rickshaw](http://code.shutterstock.com/rickshaw/)
Plotting timeseries is very common, for this purpose the spark notebook includes Rickshaw that quickly enables this feature.

Rickshaw is available through `Playground` and a dedicated function for simple needs `rickshawts`.

Thus using is rather simple and only requires to convert your type to a dedicated one `Series`:
```{scala}
def createTss(start:Long, step:Int=60*1000, nb:Int = 100):Seq[Series] = ...
val data = createTss(orig, step, nb)

val p = new Playground(data, List(Script("rickshawts", 
                                         ("renderer" -> "stack")
                                         ~ ("fixed" -> 
                                              ("interval" -> step/1000)
                                              ~ ("max" -> 100)
                                              ~ ("baseInSec" -> orig/1000)
                                           )
                                        )))(seriesCodec)
```
As you can see, the only big deal is to create the timeseries (`Seq[Series]` which is a simple wrapper around:
 * name
 * color
 * data (a sequence of `x` and `y`)

Also, you can tune a bit the display by providing:
 * the type of renderer (`line`, `stack`, ...)
 * if the timeseries will be updated you can fix the window by supplying the `fixed` object:
  * interval (at which data is upated)
  * max (the max number of points displayed)
  * the unit in the `X` axis.
 
Here is the kind of result you can expect:

![Using Rickshaw](https://raw.github.com/andypetrella/spark-notebook/spark/images/use-rickshaw.png)



## Dynamic update of data and plot using Scala's `Future`
One of the very cool things that is used in the original `scala-notebook` is the use of reactive libs on both side, server and client, combined with WebSocket, it offers a very neat way to show dynamic activities like streaming and so on.

Thanks to what is just said, we can ask Scala code to update Plot wrappers (`Playground` instance actually) in a dynamic manner. If the JS functions are listening to the data changes they can automatically update their results.

The following is showing how a timeseries shown with Rickshaw can be updated using Scala `Future` -- that simulates a server side process that would poll for a third-party service for instance:

![Update Timeseries Result](https://raw.github.com/andypetrella/spark-notebook/spark/images/dyn-ts-code.png)

The results will be:

![Update Timeseries Result](https://raw.github.com/andypetrella/spark-notebook/spark/images/dyn-ts.gif)


## Update _Notebook_ `ClassPath`
The annoying thing with notebook can be the libraries that you might miss in the classpath. The usual way to add them would be to update the SBT definition. Which is pretty sad because it requires a restart, rebuild and is not contextual to the notebook!

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

![Simple Classpath](https://raw.github.com/andypetrella/spark-notebook/spark/images/simple-cp.png)


## Update _Spark_ `jars`
For who is used with Spark, you know that it's not enough to have the jars locally added to the Driver's classpath. Indeed, workers needs to have them also. For this, there are several options, in the notebook we use the one which consist in updating the `SparkConf` with the list of jars to be added.

However, this can be very tricky when we need to add jars that have themselves a plenty of deps, for this there is a function available in the notebook context: `resolveAndAddToJars`. This function takes three parameters:
 * groupId
 * artifactId
 * version
 
What it'll do is to download the project locally with all its deps! The repo where these libs will be downloaded can be updated using the `updateRepo`. So if you want to use [Cassandra Spark connector](https://github.com/datastax/spark-cassandra-connector), all you need to do is:
```
resolveAndAddToJars("com.datastax.spark", "spark-cassandra-connector_2.10", "1.1.0-alpha3")
```
__/!\Then update the Driver cp with the listed jars.__ (This will change!)

So in live:

![Spark Jars](https://raw.github.com/andypetrella/spark-notebook/spark/images/spark-jars.png)

> Note: [Aether](https://github.com/eclipse/aether-core) is used to do this, so if there are something special needed that it's not provided, 
> we can look at how Aether would enable it!

