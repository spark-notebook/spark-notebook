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
## User/Reconfigure Spark
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

## Interacting with JavaScript

## Plotting with [D3](http://d3js.org/)

## Timeseries with  [Rickshaw](http://code.shutterstock.com/rickshaw/)

## Dynamic update of data and plot using Scala's `Future`

## Update _Notebook_ `ClassPath`

## Update _Spark_ `jars`



