package notebook.share

import java.io.File

import tachyon.client.TachyonFS
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
  val UserQuotaUnitBytes = 1000

  //Maximum number of columns allowed in RawTable, must be set on the client and server side
  val MaxColumns = 257

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

    System.setProperty("tachyon.user.quota.unit.bytes", UserQuotaUnitBytes.toString);
    System.setProperty("tachyon.max.columns", MaxColumns.toString);

    val sLocalTachyonCluster = new LocalTachyonCluster(WorkerCapacityBytes);

    sLocalTachyonCluster
  }

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
               | """.stripMargin.trim)
    sLocalTachyonCluster.start();
    println(s"Tachyon running on http://$host:$port")
    println("<<< Some properties >>>")
    println(" →) tachyon.home                                   " + System.getProperty("tachyon.home"))
    println(" →) tachyon.master.hostname                        " + System.getProperty("tachyon.master.hostname"))
    println(" →) tachyon.master.journal.folder                  " + System.getProperty("tachyon.master.journal.folder"))
    println(" →) tachyon.master.port                            " + System.getProperty("tachyon.master.port"))
    println(" →) tachyon.master.web.port                        " + System.getProperty("tachyon.master.web.port"))
    println(" →) tachyon.worker.port                            " + System.getProperty("tachyon.worker.port"))
    println(" →) tachyon.worker.data.port                       " + System.getProperty("tachyon.worker.data.port"))
    println(" →) tachyon.worker.data.folder                     " + System.getProperty("tachyon.worker.data.folder"))
    println(" →) tachyon.worker.memory.size                     " + System.getProperty("tachyon.worker.memory.size"))
    println(" →) tachyon.worker.to.master.heartbeat.interval.ms " + System.getProperty("tachyon.worker.to.master.heartbeat.interval.ms"))
    println(" →) tachyon.underfs.address                        " + System.getProperty("tachyon.underfs.address"))
    println(" →) tachyon.user.remote.read.buffer.size.byte      " + System.getProperty("tachyon.user.remote.read.buffer.size.byte"))
    println(" →) tachyon.user.quota.unit.bytes                  " + System.getProperty("tachyon.user.quota.unit.bytes"))
    println(" →) tachyon.max.columns                            " + System.getProperty("tachyon.max.columns"))
    sLocalTachyonCluster
  }

  lazy val stop: Unit = {
    sLocalTachyonCluster.stop()
    System.clearProperty("tachyon.user.quota.unit.bytes")
    System.clearProperty("tachyon.max.columns")
    ()
  }

}