import java.io.{ File, PrintWriter }
import java.nio.file.Files._
import java.nio.file.{ Files, Path, Paths }
import java.util.Calendar

import com.typesafe.scalalogging.Logger
import org.apache.poi.poifs.crypt.{ Decryptor, EncryptionInfo }
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.ss.usermodel.{ Cell, Row }
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.{ Failure, Success, Try }

trait FileImportTrait {
  var logger = Logger("FileImport")
  type RowString = String
  type CellValue = String
  type CellsArray = Array[CellValue]

  def isValidFileLocation(fileLocation: String, read: Boolean, write: Boolean): Boolean = {
    val path: Path = Paths.get(fileLocation)
    if (!exists(path) || !isDirectory(path)) {
      logger.error(s"Invalid filelocation in $fileLocation - The program exits")
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
    } else if (!Files.probeContentType(path).equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
      logger.error(s"Incorrent File Content in $file - The program exits")
      false
    } else {
      Try(new NPOIFSFileSystem(new File(s"$file"), true)) match {
        case Success(_) => true
        case Failure(e) => {
          logger.error(s"Incorrent File Content in $file ${e.getMessage}- The program exits")
          false
        }
      }
    }
  }

  def verifyPassword(file: String, password: String): Boolean = {
    val fileSystem: NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$file"), true)
    val encryptionInfo: EncryptionInfo = new EncryptionInfo(fileSystem)
    val decryptor: Decryptor = Decryptor.getInstance(encryptionInfo)
    var verificationSuccessful: Boolean = false
    Try(decryptor.verifyPassword(password)) match {
      case Success(true) => {
        true
      }
      case Success(false) => {
        logger.info("Unable to process document - incorrect password")
        false
      }
      case Failure(throwable) => {
        logger.error(throwable.getMessage)
        false
      }
    }
  }

  def getPasswordVerifiedFileAsWorkbook(fileLocation: String, password: String): XSSFWorkbook = {
    val fileSystem: NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val encryptionInfo: EncryptionInfo = new EncryptionInfo(fileSystem)
    val decryptor: Decryptor = Decryptor.getInstance(encryptionInfo)
    decryptor.verifyPassword(s"$password")
    new XSSFWorkbook(decryptor.getDataStream(fileSystem))
  }

  def readRows(workBook: XSSFWorkbook): List[RowString] = {
    val sheet: XSSFSheet = workBook.getSheetAt(0)
    val maxNumOfCells: Short = sheet.getRow(0).getLastCellNum
    val rows: Iterator[Row] = sheet.rowIterator()
    val rowBuffer: ListBuffer[RowString] = ListBuffer.empty[String]
    for (row <- rows) {
      val cells: Iterator[Cell] = row.cellIterator()
      val listOfCells: IndexedSeq[String] = for { cell <- 0 to (maxNumOfCells) } yield {
        if (row.getCell(cell) == null) {
          ""
        } else {
          row.getCell(cell).toString
        }
      }
      rowBuffer += listOfCells.mkString("|")
    }
    rowBuffer.toList
  }

  def getUser(userIdIndicator: CellValue): User = {
    userIdIndicator match {
      case BusinessUser.name => BusinessUser
      case AgentUser.name => AgentUser
      case _ => UnsupportedUser
    }
  }

  sealed trait User {
    val name: String
    val formatFunction: (CellsArray) => String

    def partitionUserAndNonUserRecords(
      rowsList: List[RowString],
      outputFileLocation: String,
      badFileLocation: String,
      currentDateTime: String,
      inputFileName: String
    ): Unit = {
      val rowsListExceptHeader: List[RowString] = rowsList.tail
      val (goodRows, badRows): (List[CellsArray], List[CellsArray]) = rowsListExceptHeader.map(rowString =>
        rowString.split("\\|")).filter(cellArray =>
        cellArray.length > 1).partition(cellArray =>
        !(cellArray(1).length == 0 || cellArray(1) == "select"))
      val goodRowsList: List[RowString] = goodRows.map(formatFunction)
      val badRowsList: List[RowString] = badRows.map(cellsArray => (s"""${cellsArray.toList}"""))
      val fileName: String = currentDateTime + inputFileName + ".txt"
      write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
      logger.info("Succesful records parsed:" + goodRowsList.length)
      logger.info("Unsuccesful records parsed:" + badRowsList.length)
    }
  }

  case object BusinessUser extends User {
    val name: String = "001"
    override val formatFunction = (cellsArray: CellsArray) => {
      (s"""${cellsArray(0)}|${cellsArray(1)}|||||||||${cellsArray(10)}|${cellsArray(11)}""")
    }
  }

  case object AgentUser extends User {
    val name: String = "002"
    override val formatFunction = (cellsArray: CellsArray) => {
      (s"""${cellsArray(0)}|${cellsArray(1)}|||||||||${cellsArray(10)}|${cellsArray(11)}|${cellsArray(12)}|||||||||${cellsArray(21)}|${cellsArray(22)}""")
    }
  }

  case object UnsupportedUser extends User {
    val name: String = "***"
    override val formatFunction = (cellsArray: CellsArray) => ""

    override def partitionUserAndNonUserRecords(fileString: List[RowString],
                                                outputFileLocation: String,
                                                badFileLocation: String,
                                                currentDateTime: String,
                                                inputFileName: String): Unit = {
      logger.info("An unrecognised file type has been encountered please see the bad output folder")
    }
  }

  protected def write(
    outputFileLocation: String,
    badFileLocation: String, goodRowsList: List[RowString], badRowsList: List[RowString], fileName: String
  ): Unit = {
    printToFile(new File(s"$badFileLocation//$fileName")) { rowString => badRowsList.foreach(rowString.println) }
    printToFile(new File(s"$outputFileLocation//$fileName")) { rowString => goodRowsList.foreach(rowString.println) }
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
      case inputFileLocation :: outputFileLocation :: badFileLocation :: inputFileName :: password :: Nil =>
        validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileName, password)
        val workbook: XSSFWorkbook = getPasswordVerifiedFileAsWorkbook(s"$inputFileLocation//$inputFileName", s"$password")
        val lineList: List[RowString] = readRows(workbook)
        val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.split("\\|"))
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
    inputFileName: String,
    password: String
  ) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    else if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    else if (!isValidFile(s"$inputFileLocation//$inputFileName")) System.exit(0)
    else if (!verifyPassword(s"$inputFileLocation//$inputFileName", s"$password")) System.exit(0)
    else
      logger.info("The input file was:" + inputFileName)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    // Mock Logger
    logger = testLogger
  }

}
