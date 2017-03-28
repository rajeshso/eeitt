import java.io.{ File, PrintWriter }
import java.nio.file.Files._
import java.nio.file.{ Files, Path, Paths }
import java.util.Calendar

import com.typesafe.scalalogging.Logger
import org.apache.poi.hssf.usermodel.{ HSSFSheet, HSSFWorkbook }
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.ss.usermodel.{ Cell, Row }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }

case class RowString(content: String) extends AnyVal
case class CellValue(content: String) extends AnyVal
trait FileImportTrait {
  var logger = Logger("FileImport")
  type CellsArray = Array[CellValue]

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
      logger.error(s"Invalid filelocation in $file - The program exits")
      false
    } else if (!isReadable(path)) {
      logger.error(s"Unable to read from $file - The program exits")
      false
    } /*else if (!Files.probeContentType(path).equals("application/vnd.ms-excel")) { //TODO this method can throw a null and is dangerous
      logger.error(s"Incorrent File Content in $file - The program exits")
      false
    }*/ else {
      Try(new NPOIFSFileSystem(new File(s"$file"), true)) match {
        case Success(_) => true
        case Failure(e) => {
          logger.error(s"Incorrent File Content in $file ${e.getMessage}- The program exits")
          false
        }
      }
    }
  }

  def fileAsWorkbook(fileLocation: String): HSSFWorkbook = {
    val fileSystem: NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$fileLocation"), false)
    new HSSFWorkbook(fileSystem)
  }

  def readRows(workBook: HSSFWorkbook): List[RowString] = {
    val sheet: HSSFSheet = workBook.getSheetAt(0)
    val maxNumOfCells: Short = sheet.getRow(0).getLastCellNum
    val rows: Iterator[Row] = sheet.rowIterator()
    val rowBuffer: ListBuffer[RowString] = ListBuffer.empty[RowString]
    for (row <- rows) {
      val cells: Iterator[Cell] = row.cellIterator()
      val listOfCells: IndexedSeq[String] = for { cell <- 0 to (maxNumOfCells) } yield {
        if (row.getCell(cell) == null) {
          ""
        } else {
          row.getCell(cell).toString
        }
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
        case cellArray if mandatoryCellsMissing(cellArray) => Array(CellValue("The length of the cells should be " + mandatorySizeOfCells + " and second & third cells should be filled")) ++ cellArray
        case cellsArray if thirdCellHasSelect(cellsArray) => Array(CellValue("The third cell has select as a value")) ++ cellsArray
        case cellsArray: CellsArray => Array(CellValue("Unknown error")) ++ cellsArray
        case _ => Array(CellValue("Unknown Error - Unable to parse the line"))
      })

      val goodRowsList: List[RowString] = goodRows.map(goodRecordFormatFunction)
      val badRowsList: List[RowString] = badRowsWithReason.map(badRecordFormatFunction)
      val fileName: String = currentDateTime + inputFileName + ".txt"
      write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
      logger.info("Succesful records parsed:" + goodRowsList.length)
      logger.info("Unsuccesful records parsed:" + badRowsList.length)
    }
    def thirdCellHasSelect(cellsArray: CellsArray): Boolean = cellsArray(2).content == "select"
    def mandatoryCellsMissing(cellsArray: CellsArray): Boolean = cellsArray.length < mandatorySizeOfCells || cellsArray(1).content.length == 0 || cellsArray(2).content.length == 0
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
    badFileLocation: String, goodRowsList: List[RowString], badRowsList: List[RowString], fileName: String
  ): Unit = {
    if (badRowsList.size != 0) printToFile(new File(s"$badFileLocation/file")) { printWriter => badRowsList.foreach(rowString => (printWriter.println(rowString.content))) } //TODO can not name a file with two conflicting extensions in windows(.xls.txt doesn't work in windows)
    if (goodRowsList.size != 0) printToFile(new File(s"$outputFileLocation/file")) { printWriter => goodRowsList.foreach(rowString => printWriter.println(rowString.content)) }
  }

  def printToFile(f: File)(op: PrintWriter => Unit) = {
    val writer: PrintWriter = new PrintWriter(f)
    try {
      op(writer)
      logger.info("The output file is " + f.getAbsoluteFile)
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    } finally {
      writer.close()
    }
  }
}

object FileImport extends FileImportTrait {
  def main(args: Array[String]): Unit = {
    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
    logger.info("Received arguments " + args.toList.toString)

    args.toList match {
      case inputFileLocation :: outputFileLocation :: badFileLocation :: inputFileName :: Nil =>
        validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileName)
        val workbook: HSSFWorkbook = fileAsWorkbook(s"$inputFileLocation/$inputFileName")
        val lineList: List[RowString] = readRows(workbook)
        val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.content.split("\\|")).map(strArray => strArray.map(str => CellValue(str)))
        val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
        val user: FileImport.User = getUser(userIdIndicator)
        user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
      case _ => logger.error("Incorrect number of arguments supplied. The program exits.")
    }
  }

  private def validateInput(
    inputFileLocation: String,
    outputFileLocation: String,
    badFileLocation: String,
    inputFileName: String
  ) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    else if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    else if (!isValidFile(s"$inputFileLocation/$inputFileName")) System.exit(0)
    else
      logger.info("The input file was:" + inputFileName)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    // Mock Logger
    logger = testLogger
  }

}
