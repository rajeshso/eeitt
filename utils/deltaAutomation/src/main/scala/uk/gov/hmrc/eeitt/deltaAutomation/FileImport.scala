package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.File
import java.nio.file.Files._
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Cell, Row, Workbook, _ }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }

//TODO Rename FileImport to FileTransformation and FileImportCLI as FileImportTransformerCLI
trait FileImport {

  System.setProperty("LOG_HOME", getClass.getResource("/Logs").getPath.drop(5))
  val logger = Logger("FileImport")

  val currentDateTime: String = getCurrentTimeStamp
  val inputFileLocation: String = initialiseFiles(getClass.getResource("/Files/Input").getPath)
  val inputFileArchiveLocation: String = initialiseFiles(getClass.getResource("/Files/Input/Archive").getPath)
  val outputFileLocation: String = initialiseFiles(getClass.getResource("/Files/Output").getPath)
  val badFileLocation: String = initialiseFiles(getClass.getResource("/Files/Bad").getPath)

  type CellsArray = Array[CellValue]

  def initialiseFiles(path: String): String = {
    if (path.contains("file:")) {
      val isFileCreated = new File(path.drop(5)).mkdirs()
      isFileCreated match {
        case true => path.drop(5)
        case false => "" //logger.error(s"the path $path was not initialised")
        // Add default value ? or shutdown
      }
    } else {
      val isFileCreated = new File(path).mkdirs()
      isFileCreated match {
        case true => path
        case false => "" //logger.error(s"the path $path was not initialised")
        // Add default value ? or shutdown ?
      }
    }
  }

  //TODO Check if this method can be moved to FileImportCLI
  def getListOfFiles(dirName: String): List[File] = {
    val directory = new File(dirName)
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
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
      Try(WorkbookFactory.create(new File(s"$file"))) match {
        case Success(_) => true
        case Failure(e) => {
          logger.error(s"Incorrent File Content in $file ${e.getMessage} - This file is not processed")
          false
        }
      }
    }
  }

  def getFileAsWorkbook(fileLocation: String): Workbook = {
    WorkbookFactory.create(new File(s"$fileLocation"))
  }

  //TODO : Add Unit test
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
