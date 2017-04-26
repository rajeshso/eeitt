package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.File
import java.nio.file.Files.{ exists, isReadable, isRegularFile }
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }

import com.typesafe.scalalogging.Logger
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.eeitt.deltaAutomation.errors.FailureReason
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService
import uk.gov.hmrc.eeitt.deltaAutomation.load.RESTClientObject

import scala.util.{ Failure, Success, Try }

trait DataValidation extends WorkBookProcessing {

  def locations: Locations
  def reader: Reader
  def logger: Logger

  def filterValid(files: List[File], validation: String => Boolean): List[File] = {
    val (goodFiles, badFiles) = files.partition(file => validation(file.getCanonicalPath))
    badFiles.foreach(file => archiveInvalidFiles(file, locations.inputFileArchiveLocation))
    goodFiles
  }

  def isGoodData(filePath: String, user: User): Boolean = {
    val result = for {
      actual <- getNumberOfUniqueUsers(filePath, user)
    } yield {
      val expected = getActualUniqueUserCount(getData(filePath, user))
      expected == actual
    }

    result match {
      case Right(x) => x
      case Left(err) =>
        logger.error(s"the dry run failed for ${err.reason}")
        false
    }
  }

  private def archiveInvalidFiles(file: File, archiveLocation: String): Unit = {
    logger.debug(s"Archived ${file.getName} to $archiveLocation")
    Files.move(file.toPath, new File(archiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  private def getNumberOfUniqueUsers(fileLocation: String, user: User): Either[FailureReason, Int] = {
    val numbers = for {
      result <- doCall(fileLocation, user)
    } yield {
      parseJsonResponse(result) match {
        case Left(_) => 0
        case Right(x) => x
      }
    }
    numbers
  }

  private def getActualUniqueUserCount(rows: List[String]): Int = {
    rows.groupBy(_.split("\\|")(1)).size
  }

  private def formatData(fileLocation: String, user: User): String = {
    getData(fileLocation, user).mkString("\n")
  }

  private def getData(fileLocation: String, user: User): List[String] = {
    reader.readDataFromFile(fileLocation, user)
  }

  private def doCall(fileLocation: String, user: User): Either[FailureReason, JsValue] = {
    val response = RESTClientObject.process(formatData(fileLocation, user), user)
    response match {
      case Right(x) => Right(Json.parse(x.body))
      case Left(err) =>
        Left(FailureReason(err))
    }
  }

  private def parseJsonResponse(json: JsValue): Either[Unit, Int] = {
    val string = (json \ "message").asOpt[String]
    string match {
      case Some(x) => Right(x.head.asDigit)
      case None =>
        Left(GMailService.sendError())
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
