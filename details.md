Spark Notebook
==============

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/andypetrella/spark-notebook.svg?branch=master)](https://travis-ci.org/andypetrella/spark-notebook)

*Originally forked from the amazing [scala-notebook](https://github.com/Bridgewater/scala-notebook), almost entirely refactored for Massive Dataset Analysis using [Apache Spark](http://spark.apache.org).*

<!-- MarkdownTOC depth=6 autolink=true bracket=round -->

- [Description](#description)
  - [Quick Start Guide](#quick-start-guide)
  - [Discussions](#discussions)
  - [Mailing list](#mailing-list)
    - [Spark Notebook Dev](#spark-notebook-dev)
    - [Spark Notebook User](#spark-notebook-user)
- [In the wild](#in-the-wild)
  - [Testimonials](#testimonials)
    - [Skymind - The Deeplearning4j](#skymind---the-deeplearning4j)
  - [Adopters](#adopters)
- [Launch](#launch)
  - [Using a release](#using-a-release)
    - [Requirements](#requirements)
    - [Preferred/Simplest way](#preferredsimplest-way)
    - [Hard ways](#hard-ways)
      - [ZIP/TGZ](#ziptgz)
      - [Docker](#docker)
        - [boot2docker (Mac OS X)](#boot2docker-mac-os-x)
        - [DEB](#deb)
    - [From the sources](#from-the-sources)
      - [Procedure](#procedure)
        - [Download the code](#download-the-code)
        - [Launch the server](#launch-the-server)
        - [Change relevant versions](#change-relevant-versions)
        - [Create your distribution](#create-your-distribution)
          - [Docker](#docker-1)
          - [Mesos in Docker](#mesos-in-docker)
        - [Customizing your build](#customizing-your-build)
          - [Update main configuration](#update-main-configuration)
          - [Update Docker configuration](#update-docker-configuration)
        - [Using unreleased Spark version](#using-unreleased-spark-version)
      - [Building for specific distributions](#building-for-specific-distributions)
        - [MapR](#mapr)
        - [Building for MapR](#building-for-mapr)
        - [Running with MapR](#running-with-mapr)
  - [Use](#use)
- [Clusters / Clouds](#clusters--clouds)
  - [Amazon EMR](#amazon-emr)
    - [Description](#description-1)
    - [Version 3.x](#version-3x)
      - [Environment](#environment)
      - [Spark Notebook](#spark-notebook)
        - [Install](#install)
        - [Configure](#configure)
        - [Run](#run)
    - [Version 4.x](#version-4x)
      - [Version 4.0](#version-40)
        - [Environment](#environment-1)
        - [Spark Notebook](#spark-notebook-1)
          - [Install](#install-1)
          - [Configure](#configure-1)
          - [Run](#run-1)
          - [Access](#access)
      - [Version 4.2](#version-42)
        - [Environment](#environment-2)
        - [Spark Notebook](#spark-notebook-2)
          - [Install](#install-2)
          - [Configure](#configure-2)
          - [Run](#run-2)
          - [Access](#access-1)
  - [Mesosphere DCOS](#mesosphere-dcos)
    - [Description](#description-2)
    - [Environment](#environment-3)
    - [Spark Notebook](#spark-notebook-3)
      - [Install](#install-3)
      - [Access](#access-2)
- [Features](#features)
  - [Configure the environment](#configure-the-environment)
    - [Using the Metadata](#using-the-metadata)
      - [Set local repository](#set-local-repository)
      - [Add remote repositories](#add-remote-repositories)
      - [Import (download) dependencies](#import-download-dependencies)
      - [Add Spark Packages](#add-spark-packages)
      - [Default import statements](#default-import-statements)
      - [Add JVM arguments](#add-jvm-arguments)
      - [Spark Conf](#spark-conf)
      - [Example](#example)
        - [YARN](#yarn)
      - [Create a preconfigured notebook](#create-a-preconfigured-notebook)
        - [Update preconfigurations in metadata](#update-preconfigurations-in-metadata)
    - [Use the `form`](#use-the-form)
    - [The `reset` function](#the-reset-function)
    - [Keep an eye on your tasks](#keep-an-eye-on-your-tasks)
  - [Using Tachyon](#using-tachyon)
    - [Connect to an existing cluster...](#connect-to-an-existing-cluster)
    - [... Embedded local (default)](#-embedded-local-default)
    - [Check using the UI](#check-using-the-ui)
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
    - [Classes required to connect to the cluster](#classes-required-to-connect-to-the-cluster)
    - [Picked first `Classpath` declaration](#picked-first-classpath-declaration)
  - [Update Spark dependencies (`spark.jars`)](#update-spark-dependencies-sparkjars)
    - [Set `local-repo`](#set-local-repo)
    - [Add `remote-repo`](#add-remote-repo)
      - [`remote-repo` with authentication](#remote-repo-with-authentication)
    - [Download and add dependencies](#download-and-add-dependencies)
      - [Local only variant `:ldp`](#local-only-variant-ldp)
- [CUSTOMIZE](#customize)
  - [Logo](#logo)
  - [Project Name](#project-name)
- [TIPS AND TROUBLESHOOTING](#tips-and-troubleshooting)
- [IMPORTANT](#important)
- [KNOWN ISSUES](#known-issues)
  - [`User limit of inotify watches reached`](#user-limit-of-inotify-watches-reached)

<!-- /MarkdownTOC -->


# Description

The main intent of this tool is to create [reproducible analysis](http://simplystatistics.org/2014/06/06/the-real-reason-reproducible-research-is-important/) using Scala, Apache Spark and more.

This is achieved through an interactive web-based editor that can combine Scala code, SQL queries, Markup or even JavaScript in a collaborative manner.

The usage of Spark comes out of the box, and is simply enabled by the implicit variable named `sparkContext`.

### Quick Start Guide

Want to try out Spark Notebook? Do these steps.

* Go to [spark-notebook.io](http://spark-notebook.io/).
* Download one of the builds.
* Expand the file somewhere convenient.
* Open a terminal/command window.
* Change to the root directory of the expanded distribution.
* Execute the command `bin/spark-notebook` (*NIX) or `bin\spark-notebook` (Windows).
* Open your browser to [localhost:9000](http://localhost:9000).

For more details on getting started, see [Launch](#launch).

### Discussions
C'mon on [gitter](https://gitter.im/andypetrella/spark-notebook?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)!

### Mailing list
There are two different mailing lists, each aiming to specific discussions:

#### Spark Notebook Dev

The [spark-notebook-dev](https://groups.google.com/forum/?hl=fr#!forum/spark-notebook-dev) mailing list for all threads regarding implementation, architecture, features and what not related to fix or enhance the project.

Email: [spark-notebook-dev@googlegroups.com](mailto:spark-notebook-dev@googlegroups.com) (go to [spark-notebook-dev](https://groups.google.com/forum/#!forum/spark-notebook-dev) to Join; carefully check the options to receive emails).

#### Spark Notebook User

The [spark-notebook-user](https://groups.google.com/forum/?hl=fr#!forum/spark-notebook-user) is for almost everything else than dev, which are questions, bugs, complains, or hopefully some kindness :-D.

Email: [spark-notebook-user@googlegroups.com](mailto:spark-notebook-user@googlegroups.com) (go to [spark-notebook-user](https://groups.google.com/forum/#!forum/spark-notebook-user) to Join; carefully check the options to receive emails).


# In the wild
## Testimonials
### Skymind - The [Deeplearning4j](http://Deeplearning4j.org)

> Spark Notebook gives us a clean, useful way to mix code and prose when we demo and explain our tech to customers. The Spark ecosystem needed this.

## Adopters

|                 Name                  |                                        Logo                                              |                     URL                     |              Description                            |
|---------------------------------------|------------------------------------------------------------------------------------------|---------------------------------------------|-----------------------------------------------------|
|            Data Fellas                | ![Data Fellas](http://www.data-fellas.guru/assets/images/logo-wired-small.png)           | [website](http://www.data-fellas.guru)      | Mad Data Science and Scalable Computing             |
|            Agile Lab                  | ![Agile Lab](http://www.agilelab.it/wp-content/uploads/2015/02/logo1.png)                | [website](http://www.agilelab.it)           | The only Italian Spark Certified systems integrator |
|            CloudPhysics               | ![CloudPhysics](https://www.cloudphysics.com/static/uploads/2014/06/3color_bug_lg.png)   | [website](http://www.cloudphysics.com)      | DATA-DRIVEN INSIGHTS FOR SMARTER IT                 |
| Aliyun | ![Alibaba - Aliyun ECS](http://gtms02.alicdn.com/tps/i2/T1J0xIFMteXXX4dCTl-220-72.png) | [product](http://market.aliyun.com/products/56014009/jxsc000194.html?spm=5176.900004.4.1.WGc3Ei) | Spark runtime environment on ECS and management tool of Spark Cluster running on Aliyun ECS  |
| EMBL European Bioinformatics Institute | ![EMBL - EBI](http://www.ebi.ac.uk/miriam/static/main/img/EBI_logo.png) | [website](http://www.ebi.ac.uk/) | EMBL-EBI provides freely available data from life science experiments, performs basic research in computational biology and offers an extensive user training programme, supporting researchers in academia and industry.  |
| Metail | ![Metail](http://metail.wpengine.com/wp-content/uploads/2013/11/Metail_Logo1.png) | [website](http://metail.com/) | The best body shape and garment fit company in the world. To create and empower everyone’s online body identity.|
| kt NexR | ![kt NexR](http://ktnexr.com/images/main/kt_h_logo.jpg) | [website](http://ktnexr.com)| the kt NexR is one of the leading BigData company in the Korea from 2007. |
| Skymind | ![Skymind](http://skymind.io/wp-content/uploads/2015/02/logo.png) | [website](http://www.skymind.io)| At Skymind, we’re tackling some of the most advanced problems in data analysis and machine intelligence. We offer start-of-the-art, flexible, scalable deep learning for industry. |
| Amino | ![Amino](https://amino.com/static/img/amino-logo-300x75.png) | [website](http://www.Amino.com)| A new way to get the facts about your health care choices. |
| Vinted | ![Vinted](http://engineering.vinted.com/brandbook/images/logos/vinted.svg) | [website](http://www.vinted.com/)| Online marketplace and a social network focused on young women’s lifestyle. |
| Vingle | ![Vingle](https://s3.amazonaws.com/vingle-assets/Vingle_Wordmark_Red.png) | [website](https://www.vingle.net)| Vingle is the community where you can meet someone like you. |
| 47 Degrees | ![47 Degrees](http://www.47deg.com/assets/logo_148x148.png) | [website](http://www.47deg.com)| 47 Degrees is a global consulting firm and certified Typesafe & Databricks Partner specializing in Scala & Spark. |

# Launch

### Using a release

Long story short, there are several ways to start the Spark Notebook quickly (even from scratch):
 * [**Preferred**] Built/Get from **[http://spark-notebook.io](http://spark-notebook.io)**
 * ZIP/TGZ file
 * Docker image
 * DEB package

However, there are several flavors for these distributions that depends on the Spark version and Hadoop version you are using.

#### Requirements
* Make sure you're running at least Java 7 (`sudo apt-get install openjdk-7-jdk`).

#### Preferred/Simplest way
Head to [http://spark-notebook.io](http://spark-notebook.io).

<p class="lead" markdown='1'>
You'll be presented a form to get the distribution you want.
If not available, it'll gracefully build it for you and notify you want it'll be ready
</p>

**IMPORTANT**: then you can check the related section for instructions on how to use it (although it's very easy).

#### Hard ways
##### ZIP/TGZ
The zip/tgz distributions are publicly available in the bucket: <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook">s3://spark-notebook</a>.

Here is an example for **zip** (replace all zip by **tgz** for the tarbal version):
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/zip/spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-1.0.4.zip
unzip spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-1.0.4.zip
cd spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-1.0.4
./bin/spark-notebook
```

##### Docker
If you're a Docker user, the following procedure will be even simpler!

**Checkout** the needed version <a href="https://registry.hub.docker.com/u/andypetrella/spark-notebook/tags/manage/">here</a>.

```
docker pull andypetrella/spark-notebook:0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.4.0
docker run -p 9000:9000 andypetrella/spark-notebook:0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.4.0
```

###### boot2docker (Mac OS X)
On Mac OS X, you need something like _boot2docker_ to use docker. However, port forwarding needs an extra command necessary for it to work (cf [this](http://stackoverflow.com/questions/28381903/spark-notebook-not-loading-with-docker) and [this](http://stackoverflow.com/questions/21653164/map-ports-so-you-can-access-docker-running-apps-from-osx-host) SO questions).

```
VBoxManage modifyvm "boot2docker-vm" --natpf1 "tcp-port9000,tcp,,9000,,9000"
```


##### DEB
Using debian packages is one of the standard, hence the Spark Notebook is also available in this form (from v0.4.0):


```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/deb/spark-notebook_0.6.0-scala-2.10.4-spark-1.4.1-hadoop-1.0.4_all.deb
sudo dpkg -i spark-notebook_0.6.0-scala-2.10.4-spark-1.4.1-hadoop-1.0.4_all.deb
sudo spark-notebook
```

**Checkout** the needed version <a href="http://s3.eu-central-1.amazonaws.com/spark-notebook/index.html">here</a>.


### From the sources
The Spark Notebook requires a [Java(TM)](http://en.wikipedia.org/wiki/Java_(programming_language)) environment (aka JVM) as runtime and [SBT](http://www.scala-sbt.org/) to build it.

Of course, you will also need a working [GIT](http://git-scm.com/) installation to download the code and build it.

#### Procedure
##### Download the code
```
git clone https://github.com/andypetrella/spark-notebook.git
cd spark-notebook
```

##### Launch the server
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


##### Change relevant versions
When using **Spark** we generally have to take a lot of care with the **Spark** version itself but also the **Hadoop** version.
There is another dependency which is tricky to update, the **jets3t** one.

To update that, you can pass those version as properties, here is an example with the current default ones:
```
sbt -D"spark.version"="1.5.0" -D"hadoop.version"="2.6.0" -D"jets3t.version"="0.7.1" -Dmesos.version="0.24.0"
```

##### Create your distribution
For a simple `zip` distro, you can run
```
[spark-notebook] $ dist
```

In order to develop on the Spark Notebook, you'll have to use the `run` command instead.

###### Mesos in Docker
By default, the docker distro will install mesos `0.22.0` (the current DCOS version), however this can be changed using the property `mesos.version`.

So if you need the mesos version `0.23.0` in your docker image you can publish it to you local machine like this:
```
sbt -Dmesos.version=0.23.0 docker:publishLocal
```

##### Customizing your build
If you want to change some build information like the `name` or the `organization` or even specific to `docker` for your builds.
Preferably you would want to avoid having to change the source code of this project.

You can do this by creating some hidden files being caught by the builder but kept out of git (added to `.gitignore`).

Here are the supported configuration.

###### Update main configuration
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

##### Using unreleased Spark version
While using the repository from SBT, you can use any version of Apache Spark you want (**up to 1.4 atm**), this will require several these things:

* you have a local install (maven) of the version (let's say Spark 1.4-RC1 or even Spark-1.4-SNAPSHOT)
* you run the sbt command using the wanted version `sbt -Dspark.version=1.4.0-SNAPSHOT`
* there is a folder in `modules/spark/src/main/scala-2.1X` that points to the base of the required version (excl. the classifier SNAPHOST or RC): `spark-1.4`

#### Building for specific distributions

##### MapR

##### Building for MapR
MapR has a custom set of Hadoop jars that must be used.  To build using these jars:
* Add the [MapR Maven repository](http://doc.mapr.com/display/MapR/Maven+Artifacts+for+MapR) as an sbt
[proxy repository](http://www.scala-sbt.org/0.13/docs/Proxy-Repositories.html)
* Build Spark Notebook using the MapR Hadoop jars by setting the `hadoop-version` property to the MapR-specific version.
See the MapR Maven Repository link above for the specific versions to use.  For example, to build Spark Notebook with
the MapR Hadoop jars and Hive and Parquet support for Spark 1.3.1:
```
sbt -Dspark.version=1.3.1 -Dhadoop.version=2.5.1-mapr-1503 -Dwith.hive=true -Dwith.parquet=true clean dist
```

##### Running with MapR
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

# Clusters / Clouds

## Amazon EMR

### Description

You can on Amazon EMR launch Spark Clusters from this [page](https://console.aws.amazon.com/elasticmapreduce/) or using the [AWS CLI](https://docs.aws.amazon.com/ElasticMapReduce/latest/DeveloperGuide/emr-spark-launch.html).

**NOTE**: For reproductability, the notebook which was already created including examples use its own metadata. Hence you will need to create a new notebook that will be applied the template from application.conf as explained below or you have to change the metadata of the exisiting one([Edit] -> [Edit Notebook Metadata]).

### Version 3.x

#### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.4.0
* Spark 1.3.1
* Hive 0.13.1
* Scala 2.10.4

#### Spark Notebook

##### Install
It's recommended to install the Spark Notebook on the master node. You will have to create your distro that copes with the environment above, but a tar version already exists [on S3 for you](https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz).

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet spark-notebook
rm spark-notebook-0.6.0-scala-2.10.4-spark-1.3.1-hadoop-2.4.0-with-hive-with-parquet.tgz
```

##### Configure

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


##### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

The port `9000` being already taken by Hadoop (hdfs), you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

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

### Version 4.x
#### Version 4.0

**Interesting page to check:** [differences with version 3](http://docs.aws.amazon.com/ElasticMapReduce/latest/ReleaseGuide/emr-release-differences.html).

##### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.6.0
* Spark 1.4.1
* Hive 1.0.0
* Scala 2.10.4

##### Spark Notebook

###### Install
It's recommended to install the Spark Notebook on the master node. You will have to create your distro that copes with the environment above, but a tar version already exists [on S3 for you](https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz).

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/emr/spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet spark-notebook
rm spark-notebook-0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.6.0-with-hive-with-parquet.tgz
```

###### Configure

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


###### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

The port `9000` being already taken by Hadoop (hdfs), you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

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

###### Access

There are several manners to access the notebook UI on the port `8989` (see above):

* easiest: `ssh -i key.pem -L 8989:localhost:8989 hadoop@<master>` then access it locally on [http://localhost:8989](http://localhost:8989)
* sustainable but unsecure: update/create the security group of the master node to open the `8989` port
* intermediate: use **FoxyProxy** in Chrome (f.i.) to redirect the url to your cluster, after having prealably open a tunnel to the master (*this is described in your cluster summary page*)

> You can also check the **YARN UI** wether your **new** notebooks are registering as applications.
>
> In version **3**, this UI is accessible from the master public DNS on port `8088`.
>
> In version **4**, this UI is accessible from the master public DNS on port `9026`.

#### Version 4.2

##### Environment
At the writing time, the created clusters has this environmnent:

* Yarn as the cluster manager
* Hadoop 2.6.0
* Spark 1.5.2
* Hive 1.0.0
* Scala 2.10.4

##### Spark Notebook

###### Install
It's recommended to install the Spark Notebook on the master node. So you can start by `ssh`'ing to it.

So when you're logged on the master, you can run:
```
wget https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet.tgz
tar xvzf spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet.tgz
mv spark-notebook-0.6.2-scala-2.10.4-spark-1.5.2-hadoop-2.6.0-with-hive-with-parquet spark-notebook
cd spark-notebook
```

###### Configure

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


###### Run

To run the notebook, it's **important** to update its classpath with the location of the configuration files for yarn, hadoop and hive, but also the different specific jars that the drivers will require to access the Yarn cluster.

The port `9000` being already taken by Hadoop (hdfs), you'll need to run it on a different port, below we've arbitrarly chosen `8989`.

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

###### Access

There are several manners to access the notebook UI on the port `8989` (see above):

* easiest: `ssh -i key.pem -L 8989:localhost:8989 hadoop@<master>` then access it locally on [http://localhost:8989](http://localhost:8989)
* sustainable but unsecure: update/create the security group of the master node to open the `8989` port
* intermediate: use **FoxyProxy** in Chrome (f.i.) to redirect the url to your cluster, after having prealably open a tunnel to the master (*this is described in your cluster summary page*)

> **YARN UI**
>
> It is available on the port `8088` of your **master**

## Mesosphere DCOS

### Description
DCOS, for Data Center Operating System, is the _the next-generation private cloud_ as stated [here](https://mesosphere.com/product/).

It is built on top of the open source Mesos but major improvements in the DCOS include its command line and web interfaces, its simple packaging and installation, and its growing ecosystem of technology partners.

That gives you access to a personal cloud on different providers like AWS, Azure (soon), GCE (soon) and so on.

Then a simplistic command line can install:

* cassandra
* kafka
* spark
* ... and the Spark Notebook

To create your own cluster on Amazon within minutes, jump to this [page](https://mesosphere.com/amazon/).

### Environment
There is not so much to do here besides following the instructions to install the CLI and access your public master interface.

### Spark Notebook

#### Install
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

#### Access
The Spark Notebook will be started on the public slave of the mesos cluster on the port `8899`. This should allow you to access it using the public DNS that the DCOS installation provides you at the end of the installation.

But there are still some problem with this DNS, hence the easiest way to open the notebook is to use the public DNS reported in you ec2 interface, so go there and look for the node having a security group public, we'll use its DNS name (`{public-dns}`).

Also the port will be dynamically assigned by Marathon, however here is a way to know it easily:

```bash
id=`dcos marathon task list /spark-notebook | tail -n 1 | tr -s ' ' | cut -d ' ' -f5`

p=`dcos marathon task show $id | jq ".ports[0]"`
```

Hence, you can access the notebook at this URL: [http://{public-dns}:$p](http://{public-dns}:$p).

The newly created notebook will be created with all required Spark Configuration to access the mesos master, to declare the executor and so forth. So nothing is required on your side, you're ready to go!

# Features

## Configure the environment
Since this project aims directly the usage of Spark, a [SparkContext](https://github.com/apache/spark/blob/master/core%2Fsrc%2Fmain%2Fscala%2Forg%2Fapache%2Fspark%2FSparkContext.scala) is added to the environment and can directly be used without additional effort.

![Example using Spark](https://raw.github.com/andypetrella/spark-notebook/master/images/simplest-spark.png)

By default, Spark will start with a regular/basic configuration. There are different ways to customize the embedded Spark to your needs.

### Using the Metadata
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

The default repositories are:
- Maven local repository (of the user account that launched the notebook server)
- Maven Central
- Spark Packages repository
- Typesafe repository
- JCenter repository

Additional repositories may be added.  While the context `:remote-repo` is available from the notebook, we can also add them right in the preconfiguration:

```json
    "customRepos"     : [
      "s3-repo % default % s3://<bucket-name>/<path-to-repo> % maven % (\"$AWS_ACCESS_KEY_ID\", \"$AWS_SECRET_ACCESS_KEY\")"
    ],
```

#### Import (download) dependencies

Adding dependencies in the classpath **and** in the spark context can be done, this way (see also `:dp`).

```json
    "customDeps"      : [
      "med-at-scale        %  ga4gh-model-java  % 0.1.0-SNAPSHOT",
      "org.apache.avro     %  avro-ipc          % 1.7.6",
      "- org.mortbay.jetty %  org.eclipse.jetty % _"
    ]
```

Alternatively, we can also use Maven coordinates:

```json
    "customDeps"      : [
      "med-at-scale:ga4gh-model-java:0.1.0-SNAPSHOT",
      "org.apache.avro:avro-ipc:1.7.6",
      "- org.mortbay.jetty:org.eclipse.jetty:_"
    ]
```

#### Add Spark Packages

Spark Notebook supports the new Spark package repository at [spark-packages.org](http://spark-packages.org).  Include a package in
your notebook by adding its coordinates to the preconfiguration:

```json
    "customDeps"      : [
      "com.databricks:spark-avro_2.10:1.0.0"
    ]
```

#### Default import statements

Some package, classes, types, functions and so forth could be automatically imported, by using:

```json
    "customImports"   : "import scala.util.Random\n",
```

#### Add JVM arguments

Each notebook is actually running in a different JVM, hence you can add some parameters (like memory tuning and so on) like this:

```json
    "customArgs"   : [
      "-Dtest=ok",
      "-Dyarn.resourcemanager.am.max-attempts=1"
    ],
```

> **NOTE**:
> Don't add classpath arguments, e.g., `["-cp", "/path/to/foo.jar"]`, as this overrides the classpath, rather than adding jars to it. Use a cell with a `:cp /path/to/foo.jar` command instead.

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
    "customArgs"   : [ "-Dtest=ok", "-Dyarn.resourcemanager.am.max-attempts=1" ],
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
      "customArgs" : [ ],
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
        "customArgs" : [ "-Dyarn.resourcemanager.am.max-attempts=1" ],
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
export HADOOP_CONF_DIR=/etc/hadoop/conf (or any other means of setting environment variables)
```
1. Prepare the application master environment by adding the variables to the customSparkConf. This is how you would add variables that are commonly added via `spark.yarn.am.extraJavaOptions`. E.g. in order to run on **Hortonworks HDP** you need to add a definition for the variable `hdp.version`:)
```
"customSparkConf" : {
  "hdp.version": "2.2.x.x-yyyy"
  "spark.app.name" : "Notebook",
  "spark.master" : "yarn-client",
  "spark.executor.memory" : "1G",
  "spark.yarn.jar" : "hdfs:///user/spark/spark-assembly.jar"
}
```
1. Start the Spark Notebook, then create notebook from example yarn cluster. After a while, spark should be initialized and `sparkContext` will be ready to use.

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

## Using Tachyon
Tachyon is a great and clean way to share results, data or process when working with Spark. Hence the Spark Notebook enables some tighter interaction with it.

### Connect to an existing cluster...
If you have a Tachyon cluster already deployed it is best to set the  `manager.tachyon.url` conf key in the `application.conf` file with its url.

Then all `SparkContext` will automatically have the tachyon configuration pointing to it and thus able to it to cache or share the results.

### ... Embedded local (default)
If no configuration is set under `manager.tachyon` conf key in the `application.conf` file (see above).

An embed Tachyon local cluster will be started for you automatically.

Hence, if you wanna use this feature a bit, we'd recommend to increase the memory allocated to the Spark Notebook server:
```
sbt -J-Xms1024m -J-Xmx5000m run
```

Or when using a distro.

```
./bin/spark-notebook -J-Xms1024m -J-Xmx5000m
```

### Check using the UI
In all notebooks, a small UI has been added in order to browse the connected Tachyon cluster, hence you don't have to quit the notebook to look at the available data or check that the data has been cached in it.

It looks like the below picture and presents some buttons:
* ![refresh](https://raw.github.com/andypetrella/spark-notebook/master/images/tachyon-refresh.png): just refresh the current view (reloading the content of the current folder)
* ![expand](https://raw.github.com/andypetrella/spark-notebook/master/images/tachyon-expand.png): allows the UI to expand in width 5 times before returning to a small-ish size
* ![list](https://raw.github.com/andypetrella/spark-notebook/master/images/tachyon-list.png): browse to the related folder

Preview:
![Preview Tachyon UI](https://raw.github.com/andypetrella/spark-notebook/master/images/tachyon-ui.png)

## Using (Spark)SQL
Spark comes with this handy and cool feature that we can write some SQL queries rather than boilerplating with
Scala or whatever code, with the clear advantage that the resulting DAG is optimized.

The Spark Notebook offers SparkSQL support.
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

Here is how this can be used, with a predefined `consoleDir` JS function ([see here](https://github.com/andypetrella/spark-notebook/blob/master/app/assets/javascripts/notebook/consoleDir.coffee)):

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
Plotting timeseries is very common, for this purpose the Spark Notebook includes Rickshaw that quickly enables handsome timeline charts.

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

Hence, a dedicated context has been added to the block, `:cp` which allows us to add specify __local paths__ to jars that will be part of the classpath.

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

### Classes required to connect to the cluster

For some Hadoop distributions extra classes are needed to connect to the cluster. In situations like this use the
`EXTRA_CLASSPATH` environment variable when starting the notebook server.  For example:
```
HADOOP_CONF_DIR=/opt/mapr/hadoop/hadoop-2.5.1/etc/hadoop EXTRA_CLASSPATH=<extra MapR jars> ./spark-notebook
```

### Picked first `Classpath` declaration
Whilst `EXTRA_CLASSPATH` is included after `HADOOP` or `YARN` ones, you might still want to add declaration that overrides all.

For that, you can use `CLASSPATH_OVERRIDES` which takes the form of a classpath entry too but will applied first.

## Update Spark dependencies (`spark.jars`)
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
:remote-repo oss-sonatype % default % https://oss.sonatype.org/content/repositories/releases/ % maven
```

Above we defined a repo named `oss-sonatype` with `default` structure at localtion `https://oss.sonatype.org/content/repositories/releases/` respecting the `maven` layout.

#### `remote-repo` with authentication

Some repos (on S3 for instance) require authentication, for this you can add them **literally** or using **env variables**:

```
:remote-repo s3-repo % default % s3://<bucket-name>/<path-to-repo> % maven % ("$AWS_ACCESS_KEY_ID", "$AWS_SECRET_ACCESS_KEY")
```



### Download and add dependencies
Adding dependencies based on the context that has been set using the above contexts (repos, ...) can be done using `:dp`.

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

#### Local only variant `:ldp`
However, if you only want to update the local path, and not the `spark.jars` configuration (to prevent them to be sent over the cluster), you can simply use: `:ldp` instead.

# CUSTOMIZE
There are two ways you can customize your Spark Notebook.

## Logo
The logo is a proprietary of the Data Fellas company (BE), but it's not mandatory for you to change it when using the tool.

However, you can still change it to put your own, this is easily achieved when running a distro.

After having unpacked the spark notebook distro, create the `public/images` folder and put your `logo.png` file in.

## Project Name
By default the project name is set to `Spark Notebook`, but you might want to clearly show your own business name in the title.

This can be configured in the `application.conf` file by updating the `manager.name` property to whatever you like.


# TIPS AND TROUBLESHOOTING

There are some common problems that users experience from time to time.
So we collected some useful tips to make your life easier:

* Spark Notebook uses old hadoop 1.0.4 by default. As notebook is a spark-driver itself, hence it defines the dependencies to be used within the cluster. This means that the `hadoop-client` has to match the cluster one, that's why we need to start the correct hadoop version in (or download the right distro),
you should start Spark Notebook with `-Dhadoop.version parameter`, like: `sbt -Dhadoop.version=2.4.0 run`
* many errors are not yet reported directly to notebook console. So, if something is wrong do not forget to look at **logs/sn-session.log** and at spark worker's logs.
* your current spark configuration is shown in **Edit > Edit Notebook Metadata**. You can make changes there instead of adding a special cell for `reset`ing default spark configuration. You can also create a template for spark configuration in a "Clusters" tab.
* some features (like switching output modes of the cell) are activated by keyboard shortcuts that are described at **Help > Keyboard Shortcuts**.
* running the dist/sbt on a different port: `./bin/spark-notebook -Dhttp.port=8888`
* running the dist/sbt on a different address: `./bin/spark-notebook -Dhttp.address=example.com`
* running the dist/sbt on a different context path: `./bin/spark-notebook -Dapplication.context=/spark-notebook`. Then you can browse [http://localhost:9000/spark-notebook](http://localhost:9000/spark-notebook). **NB**: the context path **has** to start wiht `/`.


# IMPORTANT
Some vizualizations (wisp) are currently using Highcharts which is **not** available for commercial or private usage!

If you're in this case, please to <a href="email:andy.petrella@gmail.com">contact</a> me first.


# KNOWN ISSUES

## `User limit of inotify watches reached`

When running Spark Notebook on some Linux distribs (specifically ArchLinux), you may encounter this exception:

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
