package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io._
import java.nio.file.{ Files, StandardCopyOption }
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService._
import uk.gov.hmrc.eeitt.deltaAutomation.transform.UnsupportedUser._

import scala.language.implicitConversions

trait FileTransformation extends DataValidation with Locations with IOImplementation {

  System.setProperty("LOG_HOME", getPath("/Logs"))

  val isAutomated: Boolean
  override def logger = Logger("FileImport")
  val currentDateTime: String = {
    val dateFormat = new SimpleDateFormat("EEEdMMMyyyy.HH.mm.ss.SSS")
    dateFormat.format(new Date)
  }

  override val password: String = conf.getString("password.value")

  type CellsArray = Array[CellValue]

  def process(): Unit = {
    val files: List[File] = getListOfFiles(inputFileLocation)
    logger.info(s"The following ${files.size} files will be processed ")
    implementation(files)
  }

  def implementation(files: List[File]): Unit = {
    val filesWithIndex: List[(File, Int)] = files.zipWithIndex
    filesWithIndex.foreach(x => logger.info((x._2 + 1) + " - " + x._1.getAbsoluteFile.toString))
    filterValid(files, isValidFile).map { file =>
      val lineList: List[RowString] = getRows(file)
      val user: User = getCurrentUser(lineList)
      val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = writeListsToFile(file, lineList, user, isGoodData)
      logger.info(s"""Total number of records : ${lineList.length - 1}
                  |Successful records : ${goodRowsList.length}
                  |Unsuccessful records : ${badRowsList.length}
                  |Ignored records : ${ignoredRowsList.length}""".stripMargin)
      onCompletion(file, user)
      Files.move(file.toPath, new File(inputFileArchiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  def getCurrentUser(lineList: List[RowString]): User = {
    val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.content.split("\\|")).map(strArray => strArray.map(str => CellValue(str)))
    val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
    getUser(userIdIndicator)
  }

  def onCompletion(file: File, user: User): Unit = {
    val isGoodMaster = user match {
      case AgentUser => isGoodData(masterFileLocation + "/MasterAgent", user)
      case BusinessUser => isGoodData(masterFileLocation + "/MasterBusiness", user)
    }
    if (isAutomated) {
      success(isGoodMaster, file, user)
    }
    logger.info(s"the result of the dry run is $isGoodMaster")
  }

  def success(isGood: Boolean, file: File, user: User): Unit = {
    if (isGood) {
      if (isSuccessfulTransformation(file.getName.replaceFirst("\\.[^.]+$", ".txt"))) {
        sendSuccessfulResult(user)
      } else {
        sendError()
      }
    } else {
      sendError()
    }
  }

  def isSuccessfulTransformation(fileName: String): Boolean = {
    val file = new File(getPath("/Files/Output"))
    if (file.exists && file.isDirectory) {
      val fileList = file.listFiles.filter(thing => thing.isFile).toList
      fileList.exists(f => f.getName == fileName)
    } else {
      false
    }
  }

  def getListOfFiles(dirName: String): List[File] = {
    val directory = new File(dirName)
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }
}
