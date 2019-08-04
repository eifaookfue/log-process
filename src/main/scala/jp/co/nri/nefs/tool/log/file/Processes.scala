package jp.co.nri.nefs.tool.log.file

import java.nio.file.{Files, Path, Paths}
import java.util.Date

import jp.co.nri.nefs.tool.log.common.ZipUtils

class Processes(isZip: Boolean, searchPath: Path, outputPath: Path) {
  private val regex = """TradeSheet_(OMS_.*)_(.*)_([0-9][0-9][0-9][0-9][0-9][0-9])_([0-9]*).log$""".r

  private def arrange(): Unit = {
    Files.list(searchPath).forEach(file => {
      if (file.toFile.isFile && file.getFileName.toString.endsWith(".zip")){
        val tmpDirName = "%tY%<tm%<td%<tH%<tM%<tS" format new Date()
        //val tmpDir = outputPath.resolve(tmpDirName)
        //Files.createDirectories(tmpDir)
        val tmpDir = Files.createTempDirectory(outputPath,"tmp")
        val tmpZip = tmpDir.resolve(file.getFileName)
        Files.copy(file, tmpZip)
        ZipUtils.unzip(tmpZip)
        Files.delete(tmpZip)
        Files.list(tmpDir).forEach { expanded =>
          val regex(env, computer, userName, startTime) = expanded.getFileName.toString
          val tradeDate = startTime.take(8)
          val targetDir = outputPath.resolve(tradeDate)
          Files.createDirectories(targetDir)
          val targetFile = targetDir.resolve(expanded.getFileName)
          val targetZip = getZipFile(targetFile)
          if (Files.exists(targetZip)){
            print(s"$targetZip has found, so unzipping...")
            ZipUtils.unzip(targetZip)
            println("done.")
          }
          if (Files.exists(targetFile)) {
            if (expanded.toFile.length > targetFile.toFile.length) {
              println(s"override from $expanded to $targetFile")
              Files.copy(expanded, targetFile)
            } else {
              println(s"skipped copy from $expanded to $targetFile because file already exists")
            }
          } else {
            println(s"copied from $expanded to $targetFile")
            Files.copy(expanded, targetFile)
          }
          Files.delete(expanded)
          if (isZip){
            print(s"zipping $targetFile...")
            ZipUtils.zip(targetFile)
            println("done.")
            Files.delete(targetFile)
          }

        }
        Files.delete(tmpDir)
      } else {
        println(s"$file was skipped because of non zip file.")
      }
    })

  }

  private def getZipFile(path: Path): Path = {
    val name = path.getFileName.toString
    val index = name.lastIndexOf('.')
    if (index != -1){
      val base = name.substring(0, index)
      path.getParent.resolve(base + ".zip")
    } else {
      path
    }
  }
}

object Processes {
  type OptionMap = Map[Symbol, String]
  val usage = """
        Usage: jp.co.nri.nefs.tool.log.file.Processes [--nozip] [--searchdir dir] [--outputdir dir]
        """
  def main(args: Array[String]): Unit = {
    //val defaultOptions = Map('isZip -> "TRUE", 'searchdir -> "D:\\tmp3", 'outputdir -> "D:\\tmp4")
    val defaultOptions = Map('isZip -> "TRUE")
    val options = nextOption(defaultOptions, args.toList)

    val processes = new Processes(
          getBoolean(options, 'isZip),
          Paths.get(getString(options, 'searchdir)),
          Paths.get(getString(options, 'outputdir))
    )
    processes.arrange()
  }

  def getBoolean(options: OptionMap, key: Symbol): Boolean = {
    options.get(key) match {
      case Some(s) => "TRUE" == s
      case _ =>
        println(usage)
        sys.exit(1)
    }
  }

  def getString(options: OptionMap, key: Symbol): String = {
    options.get(key) match {
      case Some(s) => s
      case _ =>
        println(usage)
        sys.exit(1)
    }
  }

  def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--nozip" :: tail =>
        nextOption(map ++ Map('isZip -> "FALSE"), tail)
      case "--searchdir" :: value :: tail =>
        nextOption(map ++ Map('searchdir -> value), tail)
      case "--outputdir" :: value :: tail =>
        nextOption(map ++ Map('outputdir -> value), tail)
      case _ => println("Unknown option")
        println(usage)
        sys.exit(1)
    }
  }

}
