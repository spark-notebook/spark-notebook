package notebook.front.widgets

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}

import notebook.Codec
import notebook.JsonCodec._
import notebook.front.SingleConnectedWidget

/* TODO → Would like to have the SparkContext wrapped in an Observable
   But this will impact the whole shebang, unless we create a macro
   that inspects the code to create the dependency graph and make it fully ractive

    -------------------------------------→---------------------------------------------
    | val r = sparkContext.makeRDD(...)  | val r = sparkContext.map(sc => sc.makeRDD) |
    | val u = r.map(d => ...)            | val u = r.map(ir => ir.map(f => ...))      |
    | out(u.collect())                   | u.subscribe(v => out(v.collect()))         |
    -------------------------------------→---------------------------------------------
  */
class Spark(val initData:SparkContext)(implicit updateSC:SparkContext => Unit)
  extends Form[SparkContext] {

  val title = "Update Spark configuration"
  val paramsCodec:Codec[SparkContext, Map[String, String]] = new Codec[SparkContext, Map[String, String]] {
    def encode(sc:SparkContext):Map[String,String] = sc.getConf.getAll.toMap
    def decode(m:Map[String,String]):SparkContext = {
      val newConf = m.foldLeft(new SparkConf)((acc, e) => acc.set(e._1, e._2))
      val sparkContext = new SparkContext(newConf) // BAD but probably temporary... hem hem
      sparkContext
    }
  }

  val update:SparkContext => SparkContext = (sc:SparkContext) => {
    data.stop()
    updateSC(sc)
    data = sc
    println(data.getConf.toDebugString)
    sc
  }

}