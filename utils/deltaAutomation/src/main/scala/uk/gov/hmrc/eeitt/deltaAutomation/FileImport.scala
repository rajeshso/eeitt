package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.File
import java.nio.file.Files._
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import org.apache.poi.poifs.crypt.{Decryptor, EncryptionInfo}
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.ss.usermodel.{Cell, Row, Workbook, _}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import uk.gov.hmrc.eeitt.deltaAutomation.FileImportCLI.{getCurrentTimeStamp, isValidFileLocation, logger}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

//TODO Rename FileImport to FileTransformation and FileImportCLI as FileImportTransformerCLI
trait FileImport {
  var logger = Logger("FileImport")

  val currentDateTime: String = getCurrentTimeStamp
  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  val conf: Config = ConfigFactory.load()
  val password = conf.getString("password.value")
  val inputFileLocation = conf.getString("location.inputfile.value")
  val inputFileArchiveLocation = conf.getString("location.inputfile.archive.value")
  val outputFileLocation = conf.getString("location.outputfile.value")
  val badFileLocation = conf.getString("location.badfile.value")
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileArchiveLocation)
  type CellsArray = Array[CellValue]

  //TODO Check if this method can be moved to FileImportCLI
  def getListOfFiles(dirName: String): List[File] = {
    val directory = new File(dirName)
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  private def validateInput(
                             inputFileLocation: String,
                             outputFileLocation: String,
                             badFileLocation: String,
                             inputFileArchiveLocation: String
                           ) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    else if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(inputFileArchiveLocation, false, true)) System.exit(0)
  }

  //TODO Check if this method can be moved to FileImportCLI
  def isValidFileLocation(fileLocation: String, read: Boolean, write: Boolean): Boolean = {
    val path: Path = Paths.get(fileLocation)
    if (!exists(path) || !isDirectory(path)) {
      logger.error(s"Invalid file location in $fileLocation ")
      false
    } else if (read && !isReadable(path)) {
      logger.error(s"Unable to read from $fileLocation ")
      false
    } else if (write && !isWritable(path)) {
      logger.error(s"Unable to write to $fileLocation ")
      false
    } else {
      true
    }
  }

  def isValidFile(file: String): Boolean = {
    val path: Path = Paths.get(file)
    if (!exists(path) || !isRegularFile(path)) {
      logger.error(s"Invalid filelocation in $file - This file is not processed")
      false
    } else if (!isReadable(path)) {
      logger.error(s"Unable to read from $file - This file is not processed")
      false
    } /*else if (!Files.probeContentType(path).equals("application/vnd.ms-excel")) { //TODO this method can throw a null and is dangerous
      logger.error(s"Incorrent File Content in $file - The program exits")
      false
    }*/ else {
      Try(getFileAsWorkbook(file)) match {
        case Success(_) => true
        case Failure(e) => {
          logger.error(s"Incorrent File Content in $file ${e.getMessage} - This file is not processed")
          false
        }
      }
    }
  }

  def getFileAsWorkbook(fileLocation: String): XSSFWorkbook = {
    val fs = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val info = new EncryptionInfo(fs)
    val d: Decryptor = Decryptor.getInstance(info)

    if (!d.verifyPassword(FileImportCLI.password)) {
      println("unable to process document incorrect password")
    }
    val wb: XSSFWorkbook = new XSSFWorkbook(d.getDataStream(fs))
    wb
  }

  def readRows(workBook: Workbook): List[RowString] = {
    val sheet: Sheet = workBook.getSheetAt(0)
    val maxNumOfCells: Short = sheet.getRow(0).getLastCellNum
    val rows: util.Iterator[Row] = sheet.rowIterator()
    val rowBuffer: ListBuffer[RowString] = ListBuffer.empty[RowString]
    for (row <- rows) {
      val cells: util.Iterator[Cell] = row.cellIterator()
      val listOfCells: IndexedSeq[String] = for { cell <- 0 to maxNumOfCells } yield {
        Option(row.getCell(cell)).map(_.toString).getOrElse("")
      }
      rowBuffer += RowString(listOfCells.mkString("|"))
    }
    rowBuffer.toList
  }

  def getUser(userIdIndicator: CellValue): User = {
    userIdIndicator.content match {
      case BusinessUser.name => BusinessUser
      case AgentUser.name => AgentUser
      case _ => UnsupportedUser
    }
  }

  def getCurrentTimeStamp: String = {
    val dateFormat = new SimpleDateFormat("EEEdMMMyyyy.HH.mm.ss.SSS")
    dateFormat.format(new Date)
  }

  //TODO: Add unit test
  def process(
    currentDateTime: String,
    inputFileLocation: String,
    inputFileArchiveLocation: String,
    outputFileLocation: String,
    badFileLocation: String
  ) = {
    val files: List[File] = getListOfFiles(inputFileLocation)
    logger.info(s"The following ${files.size} files will be processed ")
    val filesWithIndex: List[(File, Int)] = files.zipWithIndex
    for (file <- filesWithIndex) logger.info((file._2 + 1) + " - " + file._1.getAbsoluteFile.toString)
    for (file <- files if isValidFile(file.getCanonicalPath)) {
      logger.info(s"Parsing ${file.getAbsoluteFile.toString} ...")
      val workbook: Workbook = getFileAsWorkbook(file.getCanonicalPath)
      val lineList: List[RowString] = readRows(workbook)
      val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.content.split("\\|")).map(strArray => strArray.map(str => CellValue(str)))
      val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
      val user: User = getUser(userIdIndicator)
      user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, file.getAbsoluteFile.getName)
      Files.move(file.toPath, new File(inputFileArchiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }
}
