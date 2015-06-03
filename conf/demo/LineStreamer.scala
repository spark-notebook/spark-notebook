package notebook.demo

import java.io.{BufferedReader, FileReader, IOException, OutputStream}
import java.net.{ServerSocket, Socket}

/**
 *
 * cd conf/demo
 * sbt runMain notebook.demo.LineStreamer http://kdd.ics.uci.edu/databases/kddcup99/kddcup.testdata.unlabeled_10_percent.gz 8888
 */
object LineStreamer extends App {

  val file::rest = args.toList

  val port = rest.headOption.map(_.toInt).getOrElse(9999)

  val source = scala.io.Source.fromFile(file)
  val iterator = source.getLines

  println("creating a socket...")
  val servsock = new ServerSocket(port)

  try {
    println("ready to accept a new connection...")
    val sock = servsock.accept()
    val os = sock.getOutputStream()

    iterator.zipWithIndex.foreach { case (line, count) =>
      //println(line)
      val mybytearray = (line + "\n").getBytes()
      os.write(mybytearray, 0, mybytearray.length)
      if (count % 5000 == 0) {
          println(count + " -- waiting --")
          Thread.sleep(1000)
          os.flush()
      }
    }
    os.close()
    println("closing connection...")
    sock.close()
  } finally {
    println("closing server socket")
    servsock.close()
  }

}