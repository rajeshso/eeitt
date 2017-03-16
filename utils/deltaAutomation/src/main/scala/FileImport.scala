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

  def filterBusinessUser(fileString: List[String]): List[String] = {
    val deleteFirstLine: List[String] = fileString.tail
    val splitString: List[Array[String]] = deleteFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
    val parsedData: List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}"""))
    parsedData
  }

  def filterAgentUser(fileString: List[String]): List[String] = {
    val delFirstLine: List[String] = fileString.tail
    val splitString: List[Array[String]] = delFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
    val parsedData: List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}|${x(12)}|||||||||${x(21)}|${x(22)}"""))
    parsedData
  }

  def convertFileToString(workBook: XSSFWorkbook): List[String] = {
    val sheet: XSSFSheet = workBook.getSheetAt(0)
    val maxNumOfCells: Short = sheet.getRow(0).getLastCellNum
    val rows: Iterator[Row] = sheet.rowIterator()
    val rowBuffer: ListBuffer[String] = ListBuffer.empty[String]
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
  def importPasswordVerifiedFile(fileLocation: String, password: String): XSSFWorkbook = {
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
}

object FileImport extends FileImportTrait {
  def main(args: Array[String]): Unit = {

    logger.info(s"Received ${args.toList.length} arguments in ${args.toList.toString}")
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
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    if (!isValidFile(s"$inputFileLocation//$inputFileName")) System.exit(0)
    if (!verifyPassword(s"$inputFileLocation//$inputFileName", s"$password")) System.exit(0)
    val myWorkbook: XSSFWorkbook = importPasswordVerifiedFile(s"$inputFileLocation//$inputFileName", s"$password")
    val fileAsString: List[String] = convertFileToString(myWorkbook)
    val splitAgentOrBusiness: List[Array[String]] = fileAsString.map(f => f.split("\\|"))
    val agentOrBusinessUser: String = splitAgentOrBusiness.tail.head.head

    val filteredFile: List[String] = agentOrBusinessUser match {
      case "002" => filterAgentUser(fileAsString)
      case "001" => filterBusinessUser(fileAsString)
      case _ => null
    }

    printToFile(new File(s"$outputFileLocation//$currentDateTime$inputFileName.txt")) { p => filteredFile.foreach(p.println) }
  }
  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }

}
