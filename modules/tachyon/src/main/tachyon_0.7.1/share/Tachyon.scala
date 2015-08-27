package notebook.share

import java.io.File

import scala.collection.JavaConverters._

import tachyon.client.TachyonFS
import tachyon.Constants
import tachyon.master.LocalTachyonCluster

object Tachyon {

  // From tachyon 0.6.3 tests code : tachyon.client.TachyonFSTest

  //tachyon.worker.hierarchystore.level{x}.dirs.quota The quotas for all storage directories in a storage layer,
  //  which are also be delimited by comma. x represents the storage layer. Workers use the corresponding quota in
  //  the configuration for storage directories. If the quota for some storage directories are not set, the last
  //  quota will be used. There is default quota(128MB) for storage layer with alias MEM, if the quota for any
  //  other storage layer is not set, the system will report the error and exit the initialization.
  val WorkerCapacityBytes = 1024 * 1024 * 1024

  //The minimum number of bytes that will be requested from a client to a worker at a time
  val UserQuotaUnitBytes = 1 * Constants.GB

  //Maximum number of columns allowed in RawTable, must be set on the client and server side
  val MaxColumns = 257

  //USER_DEFAULT_BLOCK_SIZE_BYTE
  val UserBlockSize = Constants.GB

  lazy val home = Option(new File("./tachyon"))
    .filter(_.exists)
    .orElse(Option(new File("../tachyon")))
    .filter(_.exists)
    .getOrElse(throw new IllegalStateException(
      "Arg tachyon home has to be handled specifically... :-/ → current cwd is " + new File(".").getAbsolutePath
    ))
    .getAbsolutePath


  lazy val sLocalTachyonCluster: LocalTachyonCluster = {
    //println("TACHYON HOME IS: " + home)
    //System.setProperty("tachyon.home", home);

    //System.setProperty("tachyon.user.quota.unit.bytes", UserQuotaUnitBytes.toString);
    System.setProperty("tachyon.max.columns", MaxColumns.toString);

    val sLocalTachyonCluster = new LocalTachyonCluster(WorkerCapacityBytes, UserQuotaUnitBytes, UserBlockSize)

    sLocalTachyonCluster
  }

  lazy val masterConf = sLocalTachyonCluster.getMasterTachyonConf
  lazy val host = sLocalTachyonCluster.getMasterHostname
  lazy val port = sLocalTachyonCluster.getMasterPort

  lazy val fs: TachyonFS = TachyonFS.get("tachyon://" + host + ":" + port)

  lazy val start = {
    sLocalTachyonCluster.start()
    println(s"Tachyon running on http://$host:$port")
    println( """
               |*******************************************
               |********** STARTING TACHYON  **************
               |*******************************************
               |""".stripMargin.trim)
    sLocalTachyonCluster.start();
    println(s"Tachyon running on http://$host:$port")
    println("<<< Tachyon Conf >>>")
    val m = masterConf.toMap.asScala
    val kM = m.keys.maxBy(_.size).size
    println(masterConf.toMap.asScala.filterKeys(_.startsWith("tachyon")).toList.sortBy(_._1).map{case (k,v) => s"-) ${k.padTo(kM, " ").mkString("")}: $v"}.mkString("\n"))
    sLocalTachyonCluster
  }

  lazy val stop: Unit = {
    sLocalTachyonCluster.stop()
    System.clearProperty("tachyon.user.quota.unit.bytes")
    System.clearProperty("tachyon.max.columns")
    ()
  }

}