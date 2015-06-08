import sbt._

object ConsoleHelpers {
  val cleanAllOutputs = """
    def cleanAllOutputs = {
      import java.io.File
      import notebook._, server._, util._

      val manager = new NotebookManager("", new File("notebooks"))

      println(s"Notebook Dir: ${manager.notebookDir.getAbsolutePath}")

      def cleanOuputs(path:String) {
        println("Cleaning: " + path)
        val copied =  for {
                        n <- manager.load(path)
                        cells <- n.cells
                      } yield {
                        val cs = cells.map {
                          case c@NBSerializer.CodeCell(_, _, _, _, _, Some(outputs)) =>
                            c.copy(outputs=Some(Nil))
                          case x => x
                        }
                        n.copy(cells=Some(cs))
                      }

        copied foreach (n => manager.save(path, n, true))
      }


      def recursiveListFiles(f: File): Array[File] = {
        val these = f.listFiles
        these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
      }

      val allNbs = recursiveListFiles(manager.notebookDir).filter(x => x.isFile && x.getAbsolutePath.endsWith(".snb"))

      val allPaths = allNbs.map(x => x.getAbsolutePath.drop(manager.notebookDir.getAbsolutePath.size)).map(x => if (x.head == '/') x.drop(1) else x )

      allPaths foreach cleanOuputs
    }
  """
}