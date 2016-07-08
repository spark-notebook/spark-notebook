# Documentation

## Building for specific distributions

### MapR

### Building for MapR
MapR has a custom set of Hadoop jars that must be used.  To build using these jars:
* Add the [MapR Maven repository](http://doc.mapr.com/display/MapR/Maven+Artifacts+for+MapR) as an sbt
[proxy repository](http://www.scala-sbt.org/0.13/docs/Proxy-Repositories.html)
* Build Spark Notebook using the MapR Hadoop jars by setting the `hadoop-version` property to the MapR-specific version.
See the MapR Maven Repository link above for the specific versions to use.  For example, to build Spark Notebook with
the MapR Hadoop jars and Hive and Parquet support for Spark 1.3.1:
```
sbt -Dspark.version=1.3.1 -Dhadoop.version=2.5.1-mapr-1503 -Dwith.hive=true -Dwith.parquet=true clean dist
```

### Running with MapR
A few extra jars need to be added to the notebook classpath using the `EXTRA_CLASSPATH` environment variable:
* `commons-configuration`
* `hadoop-auth`
* `maprfs`

In addition, set the `HADOOP_CONF_DIR` environment variable to your MapR Hadoop `conf` directory.
For example:
```
HADOOP_CONF_DIR=/opt/mapr/hadoop/hadoop-2.5.1/etc/hadoop \
EXTRA_CLASSPATH=/opt/mapr/lib/commons-configuration-1.6.jar:/opt/mapr/lib/hadoop-auth-2.5.1.jar:/opt/mapr/lib/maprfs-4.1.0-mapr.jar \
./spark-notebook
```

The `java.security.auth.login.config` property needs to be added to `manager.kernel.vmArgs` in Spark Notebook's
`conf/application.conf`:
```
manager {
  kernel {
    vmArgs = ["-Djava.security.auth.login.config=/opt/mapr/conf/mapr.login.conf"]
```
Otherwise you will get an error `No login modules configured for hadoop_simple`.

Use
---
Before the first launch, it may be necessary to add some settings to `conf/application.conf`.

> **Warn:** When using a distribution, this `conf/application.conf` is already present in the installed package.
>
> However, it won't be taken into account until you include it into you launch environment. To do so, you have to > create a `conf/application.ini` file with the following content:
> ```
> -Dconfig.file=./conf/application.conf
> ```
> This allows you to have several environment that can be switched via this `ini` file
>
> A **less** cleaner way would be to launch the script like this: `./bin/spark-notebook -Dconfig.file=./conf/application.conf`

In particular `manager.kernel.vmArgs` can be used to set environment variables for the driver (e.g. `-Dhdp.version=$HDP-Version` if you want to run the Spark Notebook on a **Hortonworks** cluster). These are the settings that you would commonly pass via `spark.driver.extraJavaOptions`.

When the server has been started, you can head to the page `http://localhost:9000` and you'll see something similar to:
![Notebook list](https://raw.github.com/andypetrella/spark-notebook/master/images/list.png)

From there you can either:
 * create a new notebook or
 * launch an existing notebook

In both case, the `scala-notebook` will open a new tab with your notebook in it, loaded as a web page.

> Note: a notebook is a JSON file containing the layout and analysis blocks, and it's located
> within the project folder (with the `snb` extension).
> Hence, they can be shared and we can track their history in an SVM like `GIT`.
