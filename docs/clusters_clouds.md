# Clusters / Clouds configuration

**Table of Contents:**

   * [Amazon EMR](#amazon-emr)
      * [Version 3.x](#version-3x)
      * [Version 4.x](#version-4x)
         * [Version 4.0](#version-40)
         * [Version 4.2](#version-42)
         * [Version 4.5](#version-45)
   * [Mesosphere DCOS](#mesosphere-dcos)

# Amazon EMR

You can on Amazon EMR launch Spark Clusters from this [page](https://console.aws.amazon.com/elasticmapreduce/) or using the [AWS CLI](https://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-spark-launch.html).

**NOTE**: For reproductability, the notebook which was already created including examples use its own metadata. Hence you will need to create a new notebook that will be applied the template from application.conf as explained below or you have to change the metadata of the exisiting one([Edit] -> [Edit Notebook Metadata]).

For other EMR releases see [here](http://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-relguide-links.html) the versions included in the EMR release, and likely, all you need to change in the examples below is the versions of the dependencies.

## Version 3.x

### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.4.0
* Spark 1.3.1
* Hive 0.13.1
* Scala 2.10.4

### Spark Notebook

#### Install
It's recommended to install the Spark Notebook on the master node. You will have to create your distro that copes with the environment above, but a tar version already exists [on S3 for you](https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz).

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet spark-notebook
rm spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
```

#### Configure

In order for all notebooks to use (including newly created) the Yarn cluster, you need, and it's highly recommended, to update the `application.conf` file with the relevant Spark settings:

Edit the `conf/application.conf` file and add this configuration under the manager object (locate `custom`)
```
  custom {
    sparkConf {
      spark.local.dir="/mnt/spark,/mnt1/spark"
      spark.driver.extraClassPath="/home/hadoop/spark/conf:/home/hadoop/conf:/home/hadoop/spark/classpath/emr/*:/home/hadoop/spark/classpath/emrfs/*:/home/hadoop/share/hadoop/common/lib/*:/home/hadoop/share/hadoop/common/lib/hadoop-lzo.jar"
      spark.driver.extraJavaOptions="-Dspark.driver.log.level=DEBUG"
      spark.driver.host="<MASTER LOCAL PRIVATE IP>" # looks like ip-XXX-XXX-XXX-XXX.eu-west-1.compute.internal for instance
      spark.eventLog.dir="hdfs:///spark-logs/"
      spark.eventLog.enabled="true"
      spark.executor.extraClassPath="/home/hadoop/spark/conf:/home/hadoop/conf:/home/hadoop/spark/classpath/emr/*:/home/hadoop/spark/classpath/emrfs/*:/home/hadoop/share/hadoop/common/lib/*:/home/hadoop/share/hadoop/common/lib/hadoop-lzo.jar"
      spark.executor.extraJavaOptions="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:MaxHeapFreeRatio=70"
      spark.yarn.jar="/home/hadoop/.versions/spark-1.3.1.d/lib/spark-assembly-1.3.1-hadoop2.4.0.jar"
      spark.master="yarn-client"
    }
  }
```

> **IMPORTANT:** `<MASTER LOCAL PRIVATE IP>` has to be replaced by the private IP of your master node!

_Note_: the spark assembly is referred locally in `spark.yarn.jar`, you can also put it `HDFS` yourself and refer its path on hdfs.


#### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

If the port `9001` is already being used by another service, you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

Hence, the final launch is something like this:

```
export HADOOP_CONF_DIR=/home/hadoop/conf
export EXTRA_CLASSPATH=/home/hadoop/share/hadoop/common/lib/hadoop-lzo.jar:/home/hadoop/hive/conf
./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989
```

**NOTE**: it's better to run the notebook in a `screen` for instance, so that the shell is released and you can quit your ssh connection.
```
screen  -m -d -S "snb" bash -c 'export HADOOP_CONF_DIR=/home/hadoop/conf && export EXTRA_CLASSPATH=/home/hadoop/share/hadoop/common/lib/hadoop-lzo.jar:/home/hadoop/hive/conf && ./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989 >> nohup.out'
```

## Version 4.x
### Version 4.0

**Interesting page to check:** [differences with version 3](http://docs.aws.amazon.com/ElasticMapReduce/latest/ReleaseGuide/emr-release-differences.html).

#### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.6.0
* Spark 1.4.1
* Hive 1.0.0
* Scala 2.10.4

#### Spark Notebook

##### Install
It's recommended to install the Spark Notebook on the master node. You will have to create your distro that copes with the environment above, but a tar version already exists [on S3 for you](https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz).

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet spark-notebook
rm spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
```

##### Configure

In order for all notebooks to use (including newly created) the Yarn cluster, you need, and it's highly recommended, to update the `application.conf` file with the relevant Spark settings:

Edit the `conf/application.conf` file and add this configuration under the manager object (locate `custom`)
```
  custom {
    sparkConf {
      spark.local.dir="/mnt/spark,/mnt1/spark"
      spark.driver.log.level=INFO

      spark.driver.extraClassPath=":/usr/lib/hadoop/*:/usr/lib/hadoop/../hadoop-hdfs/*:/usr/lib/hadoop/../hadoop-mapreduce/*:/usr/lib/hadoop/../hadoop-yarn/*:/usr/lib/hadoop/../hadoop-lzo/lib/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/*"

      spark.executor.extraClassPath=":/usr/lib/hadoop/*:/usr/lib/hadoop/../hadoop-hdfs/*:/usr/lib/hadoop/../hadoop-mapreduce/*:/usr/lib/hadoop/../hadoop-yarn/*:/usr/lib/hadoop/../hadoop-lzo/lib/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/*"

      spark.driver.extraJavaOptions="-Dspark.driver.log.level=INFO -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:MaxHeapFreeRatio=70 -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M"

      spark.executor.extraJavaOptions="-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:MaxHeapFreeRatio=70 -XX:+CMSClassUnloadingEnabled"

      spark.driver.host="<MASTER LOCAL PRIVATE IP>" # looks like ip-XXX-XXX-XXX-XXX.eu-west-1.compute.internal for instance

      spark.eventLog.dir="hdfs:///var/log/spark/apps"
      spark.eventLog.enabled="true"

      spark.executor.id=driver

      spark.yarn.jar="/usr/lib/spark/lib/spark-assembly-1.4.1-hadoop2.6.0-amzn-0.jar"

      spark.master="yarn-client"

      spark.shuffle.service.enabled=true
    }
  }
```

> **IMPORTANT:** `<MASTER LOCAL PRIVATE IP>` has to be replaced by the private IP of your master node!

_Note_: the spark assembly is referred locally in `spark.yarn.jar`, you can also put it `HDFS` yourself and refer its path on hdfs.


##### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

If the port `9001` is already being used by another service, you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

Hence, the final launch is something like this:

```
export HADOOP_CONF_DIR=/etc/hadoop/conf
export SPARK_HOME=/usr/lib/spark
export SPARK_WORKER_DIR=/var/run/spark/work
export EXTRA_CLASSPATH=/usr/lib/hadoop-lzo/lib/hadoop-lzo.jar:/etc/hive/conf
./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989
```

**NOTE**: it's better to run the notebook in a `screen` for instance, so that the shell is released and you can quit your ssh connection.
```
screen  -m -d -S "snb" bash -c 'export HADOOP_CONF_DIR=/etc/hadoop/conf && export SPARK_HOME=/usr/lib/spark && export SPARK_WORKER_DIR=/var/run/spark/work && export EXTRA_CLASSPATH=/usr/lib/hadoop-lzo/lib/hadoop-lzo.jar:/etc/hive/conf && ./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989 >> nohup.out'
```

##### Access

There are several manners to access the notebook UI on the port `8989` (see above):

* easiest: `ssh -i key.pem -L 8989:localhost:8989 hadoop@<master>` then access it locally on [http://localhost:8989](http://localhost:8989)
* sustainable but unsecure: update/create the security group of the master node to open the `8989` port
* intermediate: use **FoxyProxy** in Chrome (f.i.) to redirect the url to your cluster, after having prealably open a tunnel to the master (*this is described in your cluster summary page*)

> You can also check the **YARN UI** wether your **new** notebooks are registering as applications.
>
> In version **3**, this UI is accessible from the master public DNS on port `8088`.
>
> In version **4**, this UI is accessible from the master public DNS on port `9026`.

### Version 4.2

#### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.6.0
* Spark 1.5.2
* Hive 1.0.0
* Scala 2.10.4

#### Spark Notebook

##### Install
It's recommended to install the Spark Notebook on the master node. So you can start by `ssh`'ing to it.

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet spark-notebook
cd spark-notebook
```

##### Configure

In order for all notebooks to use the Yarn cluster, edit the `application.conf` with the following:

Locate the commented key `override` and paste:
```
    override {
      sparkConf  = {

        spark.driver.extraClassPath: "/etc/hadoop/conf:/usr/lib/hadoop/*:/usr/lib/hadoop-hdfs/*:/usr/lib/hadoop-yarn/*:/usr/lib/hadoop-lzo/lib/*:/usr/share/aws/aws-java-sdk/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/*",

        spark.driver.extraJavaOptions: "-Dlog4j.configuration=file:///etc/spark/conf/log4j.properties -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:MaxHeapFreeRatio=70 -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -XX:OnOutOfMemoryError='kill -9 %p'",

        spark.driver.extraLibraryPath: "/usr/lib/hadoop/lib/native:/usr/lib/hadoop-lzo/lib/native",
        spark.driver.host: "${SPARK_LOCAL_IP}",

        spark.eventLog.dir: "hdfs:///var/log/spark/apps",
        spark.eventLog.enabled: "true",

        # spark.executor.cores: "8", #x2large

        spark.executor.extraClassPath: "/etc/hadoop/conf:/usr/lib/hadoop/*:/usr/lib/hadoop-hdfs/*:/usr/lib/hadoop-yarn/*:/usr/lib/hadoop-lzo/lib/*:/usr/share/aws/aws-java-sdk/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/*",
        spark.executor.extraJavaOptions: "-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:MaxHeapFreeRatio=70 -XX:+CMSClassUnloadingEnabled -XX:OnOutOfMemoryError='kill -9 %p'",

        spark.executor.extraLibraryPath: "/usr/lib/hadoop/lib/native:/usr/lib/hadoop-lzo/lib/native",
        spark.executor.instances: "3",

        # spark.executor.memory: "19815M",  #x2large

        spark.fileserver.host: "${SPARK_LOCAL_IP}",

        spark.history.fs.logDirectory: "hdfs:///var/log/spark/apps",
        spark.history.ui.port: "18080",

        spark.localProperties.clone: "true",

        spark.master: "yarn-client",

        spark.shuffle.service.enabled: "true",

        spark.yarn.executor.memoryOverhead: "2201",
        spark.yarn.historyServer.address: "${SPARK_LOCAL_HOSTNAME}:18080",

        spark.yarn.jar="/usr/lib/spark/lib/spark-assembly-1.5.2-hadoop2.6.0-amzn-2.jar"
      }
    }
```

> **NOTE**:
> The spark assembly is referred locally in `spark.yarn.jar`, you can also put it `HDFS` yourself and refer its path on hdfs.


##### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

If the port `9001` is already being used by another service, you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

Hence, the final launch is something like this (**check** below for how to use `screen` for persistence):
```
export SPARK_LOCAL_IP=$(ec2-metadata -o | cut -d ' ' -f2)
export SPARK_LOCAL_HOSTNAME=$(ec2-metadata -h | cut -d ' ' -f2)
export CLASSPATH_OVERRIDES=/usr/lib/hadoop-lzo/lib/hadoop-lzo.jar:/etc/hive/conf:/etc/hadoop/conf:/usr/lib/hadoop/*:/usr/lib/hadoop-hdfs/*:/usr/lib/hadoop-yarn/*:/usr/lib/hadoop-lzo/lib/*:/usr/share/aws/aws-java-sdk/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/*

source /usr/lib/spark/conf/spark-env.sh

./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989
```


> **NOTE**:
> it's better to run the notebook in a `screen` for instance, so that the shell is released and you can quit your ssh connection.
> ```
> screen  -m -d -S "snb" bash -c "export SPARK_LOCAL_IP=$(ec2-metadata -o | cut -d ' ' -f2) && export SPARK_LOCAL_HOSTNAME=$(ec2-metadata -h | cut -d ' ' -f2) && export CLASSPATH_OVERRIDES=/usr/lib/hadoop-lzo/lib/hadoop-lzo.jar:/etc/hive/conf:/etc/hadoop/conf:/usr/lib/hadoop/*:/usr/lib/hadoop-hdfs/*:/usr/lib/hadoop-yarn/*:/usr/lib/hadoop-lzo/lib/*:/usr/share/aws/aws-java-sdk/*:/usr/share/aws/emr/emrfs/conf:/usr/share/aws/emr/emrfs/lib/*:/usr/share/aws/emr/emrfs/auxlib/* && source /usr/lib/spark/conf/spark-env.sh && ./bin/spark-notebook -Dconfig.file=./conf/application.conf -Dhttp.port=8989 >> nohup.out"
> ```

##### Access

There are several manners to access the notebook UI on the port `8989` (see above):

* easiest: `ssh -i key.pem -L 8989:localhost:8989 hadoop@<master>` then access it locally on [http://localhost:8989](http://localhost:8989)
* sustainable but unsecure: update/create the security group of the master node to open the `8989` port
* intermediate: use **FoxyProxy** in Chrome (f.i.) to redirect the url to your cluster, after having prealably open a tunnel to the master (*this is described in your cluster summary page*)

> **YARN UI**
>
> It is available on the port `8088` of your **master**

### Version 4.5

#### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.7.2
* Spark 1.6.1
* Hive 1.0.0
* Scala 2.10.5

#### Spark Notebook

##### Install and Run
And launch, connect to the **master** node and execute:

```
source <(curl https://s3-us-west-1.amazonaws.com/spark-notebook-emr/4.6/emr-4.6.sh)
```

> Note: The Spark Notebook runs in a `nohup` hence you will have to kill it using its PID.

##### Access

There are several manners to access the notebook UI on the port `8989` (see above):

* easiest: `ssh -i key.pem -L 8989:localhost:8989 hadoop@<master>` then access it locally on [http://localhost:8989](http://localhost:8989)
* sustainable but unsecure: update/create the security group of the master node to open the `8989` port
* intermediate: use **FoxyProxy** in Chrome (f.i.) to redirect the url to your cluster, after having prealably open a tunnel to the master (*this is described in your cluster summary page*)

> **YARN UI**
>
> It is available on the port `8088` of your **master**

# Mesosphere DCOS

DCOS, for Data Center Operating System, is the _the next-generation private cloud_ as stated [here](https://mesosphere.com/product/).

It is built on top of the open source Mesos but major improvements in the DCOS include its command line and web interfaces, its simple packaging and installation, and its growing ecosystem of technology partners.

That gives you access to a personal cloud on different providers like AWS, Azure (soon), GCE (soon) and so on.

Then a simplistic command line can install:

* cassandra
* kafka
* spark
* ... and the Spark Notebook

To create your own cluster on Amazon within minutes, jump to this [page](https://mesosphere.com/amazon/).

## Environment
There is not so much to do here besides following the instructions to install the CLI and access your public master interface.

## Spark Notebook

### Install
It requires the DCOS CLI interface installed and configured to access your new cluster.

You'll need to add the current multiverse repo to your DCOS configuration (the Data Fellas fork until the PR is merged in the Mesosphere one).

```bash
dcos config prepend package.sources https://github.com/data-fellas/multiverse/archive/spark-notebook.zip

dcos package update --validate
```

Then, you can install the Spark Notebook, this way:
```bash
dcos package install --app spark-notebook --package-version=0.0.2
```

That's it.

### Access
The Spark Notebook will be started on the public slave of the mesos cluster on the port `8899`. This should allow you to access it using the public DNS that the DCOS installation provides you at the end of the installation.

But there are still some problem with this DNS, hence the easiest way to open the notebook is to use the public DNS reported in you ec2 interface, so go there and look for the node having a security group public, we'll use its DNS name (`{public-dns}`).

Also the port will be dynamically assigned by Marathon, however here is a way to know it easily:

```bash
id=`dcos marathon task list /spark-notebook | tail -n 1 | tr -s ' ' | cut -d ' ' -f5`

p=`dcos marathon task show $id | jq ".ports[0]"`
```

Hence, you can access the notebook at this URL: [http://{public-dns}:$p](http://{public-dns}:$p).

The newly created notebook will be created with all required Spark Configuration to access the mesos master, to declare the executor and so forth. So nothing is required on your side, you're ready to go!
