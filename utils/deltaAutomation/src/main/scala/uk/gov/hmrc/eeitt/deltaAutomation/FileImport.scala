package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.{ File, PrintWriter }
import java.nio.file.Files._
import java.nio.file.{ Files, Path, Paths, StandardCopyOption }
import java.text.SimpleDateFormat
import java.util.Date
import java.util

import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Cell, Row, Workbook, _ }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

case class RowString(content: String) extends AnyVal
case class CellValue(content: String) extends AnyVal
trait FileImportTrait {
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
      logger.error(s"Invalid file location in $fileLocation - The program exits")
      false
    } else if (read && !isReadable(path)) {
      logger.error(s"Unable to read from $fileLocation - The program exits")
      false
    } else if (write && !isWritable(path)) {
      logger.error(s"Unable to write to $fileLocation - The program exits")
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

  sealed trait User {
    val name: String
    val mandatorySizeOfCells: Int
    val goodRecordFormatFunction: (CellsArray) => RowString
    val badRecordFormatFunction = (cellsArray: CellsArray) => {
      (RowString(s"""${cellsArray.map(a => a.content).mkString("|")}"""))
    }

    def partitionUserAndNonUserRecords(
      rowsList: List[RowString],
      outputFileLocation: String,
      badFileLocation: String,
      currentDateTime: String,
      inputFileName: String
    ): Unit = {
      val rowsListExceptHeader: List[RowString] = rowsList.tail
      val (goodRows, badRows): (List[CellsArray], List[CellsArray]) = rowsListExceptHeader.map(rowString =>
        rowString.content.split("\\|")).filter(cellArray =>
        cellArray.length > 1).map(cellStringArray => cellStringArray.map(cellString => CellValue(cellString))).partition(cellArray =>
        !(mandatoryCellsMissing(cellArray) || thirdCellHasSelect(cellArray)))

      val badRowsWithReason: List[CellsArray] = badRows.map(cellsArray => cellsArray match {
        case cellArray if mandatoryCellsMissing(cellArray) => Array(
          CellValue("The length of the cells should be " + mandatorySizeOfCells +
            " and second & third cells should be filled")
        ) ++
          cellArray
        case cellsArray if thirdCellHasSelect(cellsArray) => Array(
          CellValue("The third cell is unselected")
        ) ++ cellsArray
        case cellsArray: CellsArray => Array(CellValue("Unknown error")) ++ cellsArray
      })

      val goodRowsList: List[RowString] = goodRows.map(goodRecordFormatFunction)
      val badRowsList: List[RowString] = badRowsWithReason.map(badRecordFormatFunction)
      val fileName: String = currentDateTime + inputFileName + ".txt"
      write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
      logger.info("Succesful records parsed:" + goodRowsList.length)
      logger.info("Unsuccesful records parsed:" + badRowsList.length)
    }

    def thirdCellHasSelect(cellsArray: CellsArray): Boolean = cellsArray(2).content == "select"

    def mandatoryCellsMissing(cellsArray: CellsArray): Boolean = cellsArray.length < mandatorySizeOfCells ||
      cellsArray(1).content.isEmpty ||
      cellsArray(2).content.isEmpty
  }

  case object BusinessUser extends User {
    override val name: String = "001"
    override val mandatorySizeOfCells: Int = 12
    override val goodRecordFormatFunction = (cellsArray: CellsArray) => {
      (RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}"""))
    }
  }

  case object AgentUser extends User {
    override val name: String = "002"
    override val mandatorySizeOfCells: Int = 23
    override val goodRecordFormatFunction = (cellsArray: CellsArray) => {
      (RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}|${cellsArray(12).content}|||||||||${cellsArray(21).content}|${cellsArray(22).content}"""))
    }
  }

  case object UnsupportedUser extends User {
    override val name: String = "***"
    override val mandatorySizeOfCells: Int = 0
    override val goodRecordFormatFunction = (cellsArray: CellsArray) => RowString("")

    override def partitionUserAndNonUserRecords(
      fileString: List[RowString],
      outputFileLocation: String,
      badFileLocation: String,
      currentDateTime: String,
      inputFileName: String
    ): Unit = {
      logger.info("An unrecognised file type has been encountered please see the bad output folder")
    }
  }

  protected def write(
    outputFileLocation: String,
    badFileLocation: String,
    goodRowsList: List[RowString],
    badRowsList: List[RowString],
    fileName: String
  ): Unit = {
    writeRows(s"$badFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", badRowsList, "Incorrect Rows ")
    writeRows(s"$outputFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", goodRowsList, "Correct Rows ")
  }

  private def writeRows(file: String, rowStrings: List[RowString], label: String) = {
    if (rowStrings.size != 0) printToFile(new File(file), label)({ printWriter => rowStrings.foreach(rowString => (printWriter.println(rowString.content))) })
  }

  def printToFile(f: File, label: String)(op: (PrintWriter) => Unit): Unit = {
    val writer: PrintWriter = new PrintWriter(f)
    try {
      op(writer)
      logger.info(s"The file with $label is " + f.getAbsoluteFile)
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    } finally {
      writer.close()
    }
  }
}

object FileImport extends FileImportTrait {
  def main(args: Array[String]): Unit = {
    val currentDateTime: String = getCurrentTimeStamp
    logger.info("File Import utility successfully initialized with Identity " + currentDateTime)

    val conf: Config = ConfigFactory.load();
    val inputFileLocation = conf.getString("location.inputfile.value")
    val inputFileArchiveLocation = conf.getString("location.inputfile.archive.value")
    val outputFileLocation = conf.getString("location.outputfile.value")
    val badFileLocation = conf.getString("location.badfile.value")
    logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
    validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileArchiveLocation)
    val files: List[File] = getListOfFiles(inputFileLocation)
    logger.info(s"The following ${files.size} files will be processed ")
    val filesWithIndex = files.zipWithIndex
    for (file <- filesWithIndex) logger.info(file._2 + " - "+ file._1.getAbsoluteFile.toString)
    for (file <- files if isValidFile(file.getCanonicalPath)) {
      val workbook: Workbook = fileAsWorkbook(file.getCanonicalPath)
      val lineList: List[RowString] = readRows(workbook)
      val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.content.split("\\|")).map(strArray => strArray.map(str => CellValue(str)))
      val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
      val user: FileImport.User = getUser(userIdIndicator)
      user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, file.getAbsoluteFile.getName)
      Files.move(file.toPath, new File(inputFileArchiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
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

  def getCurrentTimeStamp: String = {
    val dateFormat = new SimpleDateFormat("EEEdMMMyyyy.HH.mm.ss.SSS")
    dateFormat.format(new Date)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    // Mock Logger
    logger = testLogger
  }

}
