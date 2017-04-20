package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.File
import java.nio.file.Files.{ exists, isReadable, isRegularFile }
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }

import com.typesafe.scalalogging.Logger
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService
import uk.gov.hmrc.eeitt.deltaAutomation.load.RESTClientObject

import scala.io.Source
import scala.util.{ Failure, Success, Try }

trait DataValidation extends WorkBookProcessing {

  val locations: Locations
  val logger: Logger

  def filterValid(files: List[File], validation: String => Boolean): List[File] = {
    val (goodFiles, badFiles) = files.partition(file => validation(file.getCanonicalPath))
    badFiles.foreach(file => archiveInvalidFiles(file, locations.inputFileArchiveLocation))
    goodFiles
  }

  def isGoodData(filePath: String, user: User): Boolean = {
    val expected = getActualUniqueUserCount(getDataFromFile(filePath, user))
    val actual = getNumberOfUniqueUsers(filePath, user)
    expected == actual
  }

  private def archiveInvalidFiles(file: File, archiveLocation: String): Unit = {
    logger.debug(s"Archived ${file.getName} to $archiveLocation")
    Files.move(file.toPath, new File(archiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  private def getNumberOfUniqueUsers(fileLocation: String, user: User): Int = {
    parseJsonResponse(doCall(fileLocation, user))
  }

  private def getActualUniqueUserCount(rows: List[String]): Int = {
    rows.groupBy(_.split("\\|")(1)).size
  }

  private def formatData(fileLocation: String, user: User): String = {
    getDataFromFile(fileLocation, user).mkString("\n")
  }

  private def doCall(fileLocation: String, user: User): JsValue = {
    Json.parse(RESTClientObject.process(formatData(fileLocation, user), user).body)
  }

  private def getDataFromFile(fileLocation: String, user: User): List[String] = {
    logger.debug(s" The File Location is : $fileLocation")
    Source.fromFile(fileLocation).getLines().toList.filter(_.startsWith(user.name))
  }

  private def parseJsonResponse(json: JsValue): Int = {
    val string = (json \ "message").asOpt[String]
    string match {
      case Some(x) => x.head.asDigit
      case None =>
        GMailService.sendError()
        throw new IllegalArgumentException
    }
  }

  protected def isValidFile(file: String): Boolean = {
    val path: Path = Paths.get(file)
    if (!exists(path) || !isRegularFile(path)) {
      logger.error(s"Invalid filelocation in $file - This file is not processed")
      false
    } else if (!isReadable(path)) {
      logger.error(s"Unable to read from $file - This file is not processed")
      false
    } /*else if (!Files.probeContentType(path).equals("application/vnd.ms-excel")) { //TODO this fragment can throw a null
      logger.error(s"Incorrent File Content in $file - The program exits")
      false
    }*/ else {
      Try(getFileAsWorkbook(file)) match {
        case Success(_) => true
        case Failure(e) =>
          logger.error(s"Incorrent File Content in $file ${e.getMessage} - This file is not processed")
          false
      }
    }
  }
}
