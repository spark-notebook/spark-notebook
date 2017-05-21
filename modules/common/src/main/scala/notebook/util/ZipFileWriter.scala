package notebook.util

import java.io.File


class ZipFileWriter(val name: String,
                    val tmpDir: File = { new File(sys.props("java.io.tmpdir"), "zipfile-"+System.nanoTime) }) {
  val zipFile = new File(tmpDir, name)
  zipFile.createNewFile
  protected val baos = new java.io.FileOutputStream(zipFile)
  val zip = new java.util.zip.ZipOutputStream(baos)

  /**
    * e.g.
    * write(files = List(sameFile), prefix = "")
    * write(files, "images/")
    */
  def write(files: Seq[File], prefix:String): Unit = {
    files.foreach { f =>
      zip.putNextEntry(new java.util.zip.ZipEntry(prefix+f.getName))
      val in = new java.io.BufferedInputStream(new java.io.FileInputStream(f))
      var b = in.read()
      while (b > -1) {
        zip.write(b)
        b = in.read()
      }
      in.close()
      zip.closeEntry()
    }
  }

  def close(): Unit = {
    zip.close()
  }
}
