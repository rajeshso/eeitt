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

trait FileImportTrait {
  var logger = Logger("FileImport")
  type RowString = String
  type CellValue = String
  type CellsArray = Array[CellValue]

  def printToFile(f: File)(op: PrintWriter => Unit) {
    val p: PrintWriter = new PrintWriter(f)
    try {
      op(p)
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    } finally {
      logger.info("The output file is " + f.getAbsoluteFile)
      p.close()
    }
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

  def getPasswordVerifiedFileAsWorkbook(fileLocation: String, password: String): XSSFWorkbook = {
    val fileSystem: NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val encryptionInfo: EncryptionInfo = new EncryptionInfo(fileSystem)
    val decryptor: Decryptor = Decryptor.getInstance(encryptionInfo)
    decryptor.verifyPassword(s"$password")
    new XSSFWorkbook(decryptor.getDataStream(fileSystem))
  }

  def verifyPassword(file: String, password: String): Boolean = {
    val fileSystem: NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$file"), true)
    val encryptionInfo: EncryptionInfo = new EncryptionInfo(fileSystem)
    val decryptor: Decryptor = Decryptor.getInstance(encryptionInfo)
    var verificationSuccessful: Boolean = false
    try {
      verificationSuccessful = decryptor.verifyPassword(s"$password")
      if (!verificationSuccessful) {
        logger.info("Unable to process document - incorrect password")
      }
    } catch {
      case t: Throwable => logger.error(t.getMessage)
    }
    verificationSuccessful
  }

  def isValidFileLocation(fileLocation: String, read: Boolean, write: Boolean): Boolean = {
    val path: Path = Paths.get(fileLocation)
    if (!exists(path) || !isDirectory(path)) {
      logger.error(s"Invalid filelocation in $fileLocation - The program exits")
      return false
    }
    if (read && !isReadable(path)) {
      logger.error(s"Unable to read from $fileLocation - The program exits")
      return false
    }
    if (write && !isWritable(path)) {
      logger.error(s"Unable to write to $fileLocation - The program exits")
      return false
    }
    return true
  }

  def isValidFile(file: String): Boolean = {
    val path: Path = Paths.get(file)
    if (!exists(path) || !isRegularFile(path)) {
      logger.error(s"Invalid filelocation in $file - The program exits")
      return false
    }
    if (!isReadable(path)) {
      logger.error(s"Unable to read from $file - The program exits")
      return false
    }
    if (!Files.probeContentType(path).equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
      logger.error(s"Incorrent File Content in $file - The program exits")
      return false
    }
    try {
      new NPOIFSFileSystem(new File(s"$file"), true)
    } catch {
      case e: Throwable =>
        logger.error(s"Incorrent File Content in $file ${e.getMessage}- The program exits")
        return false
    }
    return true
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

    def partitionUserAndNonUserRecords(rowsList: List[RowString], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit
  }

  case object BusinessUser extends User {
    val name: String = "001"

    override def partitionUserAndNonUserRecords(rowsList: List[RowString], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      val rowsListExceptHeader: List[RowString] = rowsList.tail
      val (goodRows, badRows): (List[CellsArray], List[CellsArray]) = rowsListExceptHeader.map(rowString => rowString.split("\\|")).partition(cellArray => !(cellArray(1) == "" || cellArray(1) == "select"))
      val goodRowsList: List[RowString] = goodRows.map(cellsArray => (s"""${cellsArray(0)}|${cellsArray(1)}|||||||||${cellsArray(10)}|${cellsArray(11)}"""))
      val badRowsList: List[RowString] = badRows.map(cellsArray => (s"""${cellsArray.toList}"""))
      val fileName: String = currentDateTime + inputFileName + ".txt"
      write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
      logger.info("Succesful records parsed:" + goodRowsList.length)
      logger.info("Unsuccesful records parsed:" + badRowsList.length)
    }
  }

  case object AgentUser extends User {
    val name: String = "002"

    override def partitionUserAndNonUserRecords(rowsList: List[RowString], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      val rowsListExceptHeader: List[RowString] = rowsList.tail
      val (goodRows, badRows): (List[CellsArray], List[CellsArray]) = rowsListExceptHeader.map(rowString => rowString.split("\\|")).partition(cellArray => !(cellArray(1) == "" || cellArray(1) == "select"))
      val goodRowsList: List[String] = goodRows.map(cellsArray => (s"""${cellsArray(0)}|${cellsArray(1)}|||||||||${cellsArray(10)}|${cellsArray(11)}|${cellsArray(12)}|||||||||${cellsArray(21)}|${cellsArray(22)}"""))
      val badRowsList: List[String] = badRows.map(cellsArray => (s"""${cellsArray.toList}"""))
      val fileName: String = currentDateTime + inputFileName + ".txt"
      write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
      logger.info("Succesful records parsed:" + goodRowsList.length)
      logger.info("Unsuccesful records parsed:" + badRowsList.length)
    }
  }

  case object UnsupportedUser extends User {
    val name: String = "***"

    override def partitionUserAndNonUserRecords(fileString: List[RowString], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      logger.info("An unrecognised file type has been encountered please see the bad output folder")
    }
  }

  protected def write(outputFileLocation: String, badFileLocation: String, goodRowsList: List[RowString], badRowsList: List[RowString], fileName: String): Unit = {
    printToFile(new File(s"$badFileLocation//$fileName")) { rowString => badRowsList.foreach(rowString.println) }
    printToFile(new File(s"$outputFileLocation//$fileName")) { rowString => goodRowsList.foreach(rowString.println) }
  }
}

object FileImport extends FileImportTrait {
  def main(args: List[String]): Unit = {
    commandLineArgs(args)

    logger.info("Received arguments " + args.toString())
    if (args.length < 5) {
      logger.error("Incorrect number of arguments supplied. The program exits.")
      System.exit(0)
    }

    def commandLineArgs(list: List[String]): Unit = {
      list match {
        case inputFileLocation :: outputFileLocation :: badFileLocation :: inputFileName :: password :: Nil =>
          validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileName, password)
          val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
          logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
          val workbook: XSSFWorkbook = getPasswordVerifiedFileAsWorkbook(s"$inputFileLocation//$inputFileName", s"$password")
          val lineList: List[RowString] = readRows(workbook)
          val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.split("\\|"))
          val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
          val user: FileImport.User = getUser(userIdIndicator)
          user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
        case _ => logger.info("Error in input args")
      }
    }
  }

  private def validateInput(inputFileLocation: String, outputFileLocation: String, badFileLocation: String, inputFileName: String, password: String) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    if (!isValidFile(s"$inputFileLocation//$inputFileName")) System.exit(0)
    if (!verifyPassword(s"$inputFileLocation//$inputFileName", s"$password")) System.exit(0)
    logger.info("The input file was:" + inputFileName)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }
}
