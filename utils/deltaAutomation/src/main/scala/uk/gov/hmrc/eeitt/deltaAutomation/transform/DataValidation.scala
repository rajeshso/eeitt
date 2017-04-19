package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.File
import java.nio.file.{ Files, StandardCopyOption }

import com.typesafe.scalalogging.Logger
import play.api.libs.json.{ JsValue, Json }
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService

import scala.io.Source
import scala.sys.process.stringSeqToProcess

trait DataValidation {

  private val logger = Logger("Data Validation")

  def filterValid(files: List[File], archiveLocation: String, validation: String => Boolean): List[File] = {
    val (goodFiles, badFiles) = files.partition(file => validation(file.getCanonicalPath))
    badFiles.foreach(file => archiveInvalidFiles(file, archiveLocation))
    goodFiles
  }

  def isGoodData(goodRows: List[RowString], filePath: String): Boolean = {
    val expected = getActualUniqueUserCount(goodRows)
    val actual = getDryRunUnqiueUserCount(filePath)
    expected == actual
  }

  private def archiveInvalidFiles(file: File, archiveLocation: String): Unit = {
    logger.debug(s"Archived ${file.getName} to $archiveLocation")
    Files.move(file.toPath, new File(archiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
  }

  private def getDryRunUnqiueUserCount(filePath: String): Int = {
    val response = doDryRun(filePath)
    parseJsonResponse(response)
  }

  //Full Master Dry Run
  def test(fileLocation: String, user: User): List[String] = {
    Source.fromFile(fileLocation + "/Master").getLines.filter(_.startsWith(user.name)).toList
  }

  def parseMaster(list: List[String]): Int = {
    list.groupBy(_.split("\\|")(1)).size
  }

  //Single File Dry Run
  private def getActualUniqueUserCount(goodRows: List[RowString]): Int = {
    goodRows.groupBy(_.content.split("\\|")(1)).size
  }

  def doDryRun(fileLocation: String): JsValue = {
    Json.parse(Seq("./DryRun.sh", "agents", fileLocation).!!)
  }

  def parseJsonResponse(json: JsValue): Int = {
    val string = (json \ "message").asOpt[String]
    string match {
      case Some(x) => x.head.asDigit
      case None =>
        GMailService.sendError()
        throw new IllegalArgumentException
    }
  }
}
