# Documentation

## Using a release

Long story short, there are several ways to start the Spark Notebook quickly:
 * [**Preferred**] Built/Get from **[http://spark-notebook.io](http://spark-notebook.io)**
 * ZIP/TGZ file
 * Docker image
 * DEB package

However, there are several flavors for these distributions that depends on the Spark version and Hadoop version you are using.

### Requirements
* Make sure you're running at least Java 7 (`sudo apt-get install openjdk-7-jdk`).

### Preferred way
Head to [http://spark-notebook.io](http://spark-notebook.io) and download a pre-built/pre-packaged installation for your specific configuration and format of choice. On [http://spark-notebook.io](http://spark-notebook.io) it is possible to requests builds for specific combinations of Scala and Spark versions, different Hadoop releases and add/remove support for Hive and Parquet. 

<p class="lead" markdown='1'>
You'll be presented a form to get the distribution you want.
If not available, it'll gracefully build it for you and notify you want it'll be ready
</p>

**Check the related section for instructions on how to use it (although it's very easy).**

#### ZIP/TGZ

Here is an example for **zip**

```bash
# Downloaded spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.zip
unzip spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.zip
cd spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0
./bin/spark-notebook
```

Here is an example for **tgz** "tarbal"

```bash
# Downloaded spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.zip
tar -xvf spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.tgz
cd spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0
./bin/spark-notebook
```

#### Docker
If you're a Docker user, the following procedure will be even simpler!

**Checkout** the needed version <a href="https://registry.hub.docker.com/u/andypetrella/spark-notebook/tags/manage/">here</a>.

```bash
docker pull andypetrella/spark-notebook:0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.4.0
docker run -p 9000:9000 andypetrella/spark-notebook:0.6.0-scala-2.10.4-spark-1.4.1-hadoop-2.4.0
```

##### Note: boot2docker (Mac OS X)
On Mac OS X, you need something like _boot2docker_ to use docker. However, port forwarding needs an extra command necessary for it to work (cf [this](http://stackoverflow.com/questions/28381903/spark-notebook-not-loading-with-docker) and [this](http://stackoverflow.com/questions/21653164/map-ports-so-you-can-access-docker-running-apps-from-osx-host) SO questions).

```bash
VBoxManage modifyvm "boot2docker-vm" --natpf1 "tcp-port9000,tcp,,9000,,9000"
```


#### DEB
For Debian/Ubuntu based distros, the Spark Notebook is also avaiable as a Debian package for download. After downloading, use your package manager to install 

```bash
# Downloaded spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.deb
sudo dpkg -i spark-notebook-0.6.0-scala-2.10.5-spark-1.6.1-hadoop-2.0.0.deb
sudo spark-notebook
```