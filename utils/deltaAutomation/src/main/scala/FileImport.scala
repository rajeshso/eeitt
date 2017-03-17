import java.io.{ File, PrintWriter }
import java.util.Calendar
import java.nio.file.{ Files, Path, Paths }
import java.nio.file.Files._

import com.typesafe.scalalogging.Logger
import org.apache.poi.poifs.crypt.{ Decryptor, EncryptionInfo }
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.ss.usermodel.{ Cell, Row }
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook }

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

trait FileImportTrait {
  var logger = Logger("FileImport")
  type RowString =  String
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

  def filterBadFile(fileString: List[String]): List[String] = {
    val deleteFirstLine: List[String] = fileString.tail
    val splitString: List[Array[String]] = deleteFirstLine.map(f => f.split("\\|"))
    val parsedData: List[String] = splitString.map(x => (s"""${x.toList}"""))
    parsedData
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

  def getUser(userIdIndicator: CellValue) : User = {
    userIdIndicator match {
      case BusinessU.name => BusinessU
      case AgentU.name => AgentU
      case _ => UnknownU
    }
  }

  sealed trait User {
    val name: String
    def partitionUserAndNonUserRecords(fileString: List[String], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit
  }

  case object BusinessU extends User {
    val name: String = "001"

    override def partitionUserAndNonUserRecords(fileString: List[String], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      val deleteFirstLine: List[String] = fileString.tail
      val (splitString, badRecord): (List[Array[String]], List[Array[String]]) = deleteFirstLine.map(f => f.split("\\|")).partition(f => !(f(1) == "" || f(1) == "select"))
      val parsedData: List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}"""))
      val badRecordParsed: List[String] = badRecord.map(x => (s"""${x.toList}"""))
      printToFile(new File(s"$badFileLocation//$currentDateTime$inputFileName.txt")) { p => badRecordParsed.foreach(p.println) }
      printToFile(new File(s"$outputFileLocation//$currentDateTime$inputFileName.txt")) { p => parsedData.foreach(p.println) }
    }
  }

  case object AgentU extends User {
    val name: String = "002"

    override def partitionUserAndNonUserRecords(fileString: List[String], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      val deleteFirstLine: List[String] = fileString.tail
      val (splitString, badRecord): (List[Array[String]], List[Array[String]]) = deleteFirstLine.map(f => f.split("\\|")).partition(f => !(f(1) == "" || f(1) == "select"))
      val parsedData: List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}|${x(12)}|||||||||${x(21)}|${x(22)}"""))
      val badRecordParsed: List[String] = badRecord.map(x => (s"""${x.toList}"""))
      printToFile(new File(s"$badFileLocation//$currentDateTime$inputFileName.txt")) { p => badRecordParsed.foreach(p.println) }
      printToFile(new File(s"$outputFileLocation//$currentDateTime$inputFileName.txt")) { p => parsedData.foreach(p.println) }
    }
  }

  case object UnknownU extends User {
    val name: String = "***"

    override def partitionUserAndNonUserRecords(fileString: List[String], outputFileLocation: String, badFileLocation: String, currentDateTime: String, inputFileName: String): Unit = {
      logger.info("An unrecognised file type has been encountered please see the bad output folder")
    }
  }
}

object FileImport extends FileImportTrait {
  def main(args: Array[String]): Unit = {

    logger.info("Received arguments " + args.toList.toString())
    if (args.length < 5) {
      logger.error("Incorrect number of arguments supplied. The program exits.")
      System.exit(0)
    }
    val inputFileLocation: String = args.apply(0)
    val outputFileLocation: String = args.apply(1)
    val badFileLocation: String = args.apply(2)
    val inputFileName: String = args.apply(3)
    val password: String = args.apply(4)
    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
    validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileName, password)
    val workbook: XSSFWorkbook = getPasswordVerifiedFileAsWorkbook(s"$inputFileLocation//$inputFileName", s"$password")
    val lineList: List[RowString] = readRows(workbook)
    val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.split("\\|"))
    val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
    val user: FileImport.User = getUser(userIdIndicator)
    user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
  }

  private def validateInput(inputFileLocation: String, outputFileLocation: String, badFileLocation: String, inputFileName: String, password: String) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    if (!isValidFile(s"$inputFileLocation//$inputFileName")) System.exit(0)
    if (!verifyPassword(s"$inputFileLocation//$inputFileName", s"$password")) System.exit(0)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }

}
