package notebook.util

import java.io.File
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.io.BufferedSource
import scala.io.Codec
import java.io.{File, FileInputStream, FileOutputStream, InputStream,
IOException, OutputStream}
import java.net.URI
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}


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


// this support zipping dirs too
// based on
// https://github.com/dhbikoff/Scala-Zip-Archive-Util/blob/dfdb96eff2230ea8e8825f1baa161a3d00e60e13/ZipArchiveUtil.scala
// FIXME: check is this can be more useful https://github.com/pathikrit/better-files#zip-apis
object ZipArchiveUtil {
  def createArchiveRecursively(rootPath: String,
                               inputFiles: Seq[String],
                               outputFilename: String): Unit = {
    println(
      s"rootPath: $rootPath\n" +
      s"inputFiles: $inputFiles\n" +
      s"outputFilename: $outputFilename"
    )
    val filePaths = inputFiles.map { inputFilePath =>
      createFileList(new File(inputFilePath).getAbsoluteFile, outputFilename)
    }.reduce(_ ++ _)

    createZip(filePaths, outputFilename, rootPath)
  }

  def createFileList(file: File, outputFilename: String): List[String] = {
    file match {
      case file if file.isFile => {
        if (file.getName != outputFilename)
          List(file.getAbsoluteFile.toString)
        else
          List()
      }
      case file if file.isDirectory => {
        val fList = file.list
        // Add all files in current dir to list and recur on subdirs
        fList.foldLeft(List[String]())((pList: List[String], path: String) =>
          pList ++ createFileList(new File(file, path), outputFilename))
      }
      case _ => throw new IOException("Bad path. No file or directory found.")
    }
  }

  def addFileToZipEntry(filename: String, parentPath: String,
                        filePathsCount: Int): ZipEntry = {
    if (filePathsCount <= 1)
      new ZipEntry(new File(filename).getName)
    else {
      // use relative path to avoid adding absolute path directories
      val relative = new File(parentPath).toURI.
        relativize(new File(filename).toURI).getPath
      new ZipEntry(relative)
    }
  }

  def createZip(filePaths: List[String], outputFilename: String,
                parentPath: String) = {
    try {
      val fileOutputStream = new FileOutputStream(parentPath + "/"+ outputFilename)
      val zipOutputStream = new ZipOutputStream(fileOutputStream)

      filePaths.foreach((name: String) => {
        println("adding " + name)
        val zipEntry = addFileToZipEntry(name, parentPath, filePaths.size)
        zipOutputStream.putNextEntry(zipEntry)
        val inputSrc = new BufferedSource(
          new FileInputStream(name))(Codec.ISO8859)
        inputSrc foreach { c: Char => zipOutputStream.write(c) }
        inputSrc.close
      })

      zipOutputStream.closeEntry
      zipOutputStream.close
      fileOutputStream.close

    } catch {
      case e: IOException => {
        e.printStackTrace
      }
    }
  }

  def unzip(file: File): Unit = {
    val basename = file.getName.substring(0, file.getName.lastIndexOf("."))
    val destDir = new File(file.getParentFile, basename)
    destDir.mkdirs

    val zip = new ZipFile(file)
    zip.entries foreach { entry =>
      val entryName = entry.getName
      val entryPath = {
        if (entryName.startsWith(basename))
          entryName.substring(basename.length)
        else
          entryName
      }

      // create output directory if it doesn't exist already
      val splitPath = entry.getName.split(File.separator).dropRight(1)
      if (splitPath.size >= 1) {
        // create intermediate directories if they don't exist
        val dirBuilder = new StringBuilder(destDir.getName)
        splitPath.foldLeft(dirBuilder)((a: StringBuilder, b: String) => {
          val path = a.append(File.separator + b)
          val str = path.mkString
          if (!(new File(str).exists)) {
            new File(str).mkdir
          }
          path
        })
      }

      // write file to dest
      println("Extracting " + destDir + File.separator + entryPath)
      val inputSrc = new BufferedSource(
        zip.getInputStream(entry))(Codec.ISO8859)
      val ostream = new FileOutputStream(new File(destDir, entryPath))
      inputSrc foreach { c: Char => ostream.write(c) }
      inputSrc.close
      ostream.close
    }
  }
}