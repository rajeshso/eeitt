package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.File

import com.typesafe.config.{ Config, ConfigFactory }

trait Locations {

  val conf: Config = ConfigFactory.load()
  val inputFileLocation: String = getFileLocation("location.inputfile.value", "/Files/Input")
  val inputFileArchiveLocation: String = getFileLocation("location.inputfile.archive.value", "/Files/Input/Archive")
  val outputFileLocation: String = getFileLocation("location.outputfile.value", "/Files/Output")
  val badFileLocation: String = getFileLocation("location.badfile.value", "/Files/Bad")

  protected def getPath(location: String): String = {
    val path = getClass.getResource(location).getPath
    if (path.contains("file:")) {
      path.drop(5)
    } else {
      path
    }
  }

  private def initialiseFiles(path: String): Unit = {
    new File(path).mkdirs()
  }

  private def getFileLocation(configValue: String, pathValue: String): String = {
    if (conf.getString(configValue).equals("DEFAULT")) {
      val path = getPath(pathValue)
      initialiseFiles(path)
      path
    } else {
      conf.getString(configValue)
    }
  }
}
