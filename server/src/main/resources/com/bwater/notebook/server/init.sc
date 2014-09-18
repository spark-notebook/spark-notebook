/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

import java.io.{FileReader, BufferedReader}

import com.bwater.notebook._, widgets._, d3._
import com.bwater.notebook.client.SparkClassServerUri
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.SparkILoop


@transient var execUri = System.getenv("SPARK_EXECUTOR_URI")
@transient var sparkHome = System.getenv("SPARK_HOME")
@transient var sparkMaster = System.getenv("MASTER")
@transient var jars = SparkILoop.getAddedJars

@transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

@transient var conf = new SparkConf()

def reset(assign:Boolean=true):SparkContext = {
  conf = new SparkConf()
  conf.setMaster(Option(sparkMaster).getOrElse("local[*]"))
      .setAppName("Notebook")
      .set("spark.repl.class.uri", uri)
  if (execUri != null) {
    conf.set("spark.executor.uri", execUri)
  }
  if (sparkHome != null) {
    conf.setSparkHome(sparkHome)
  }
  conf.setJars(jars)
  val sc = new SparkContext(conf)
  if (assign) sparkContext = sc
  sc
}

object Repos {
  import org.sonatype.aether.repository.RemoteRepository
  val central = new RemoteRepository(
    "maven-central",
    "default",
    "http://repo1.maven.org/maven2/"
  )
}

def resolveAndAddToJars(group:String, artifact:String, version:String) = {
  import com.jcabi.aether.Aether
  import java.io.File
  import java.util.Arrays
  import org.apache.maven.project.MavenProject
  import org.sonatype.aether.artifact.Artifact
  import org.sonatype.aether.util.artifact.DefaultArtifact
  import scala.collection.JavaConversions._
  //import java.nio.Files

  //val repo = Files.createTempDirectory(s"scala-notebook-aether-$group_$artifact_$version")
  val repo = new File(System.getProperty("java.io.tmpdir")+ s"/scala-notebook/aether/${group}_${artifact}_${version}/" + java.util.UUID.randomUUID.toString)

  repo.mkdirs

  val remotes = List(Repos.central)
  val deps:Set[Artifact] =  new Aether(remotes, repo).resolve(
                              new DefaultArtifact(group, artifact, "", "jar", version), 
                              "runtime"
                            ).toSet;

  val newJars = deps.map(_.getFile.getPath).toSet.toList

  jars = (newJars ::: jars.toList).distinct.toArray
}

@transient var sparkContext:SparkContext = reset(false)
