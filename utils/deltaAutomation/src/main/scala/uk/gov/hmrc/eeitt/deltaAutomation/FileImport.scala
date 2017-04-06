package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.File
import java.nio.file.Files._
import java.nio.file.{ Path, Paths }
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Cell, Row, Workbook, _ }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }

trait FileImport {
  var logger = Logger("FileImport")

  type CellsArray = Array[CellValue]

  def getListOfFiles(dirName: String): List[File] = {
    val directory = new File(dirName)
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

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

  def fileAsWorkbook(fileLocation: String): Workbook = {
    val fileSystem = WorkbookFactory.create(new File(s"$fileLocation"))
    fileSystem

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
}
