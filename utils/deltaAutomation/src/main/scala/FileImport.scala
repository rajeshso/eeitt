import java.io.File
import java.util.{ Calendar, Date }
import java.io.PrintWriter

import org.apache.poi.poifs.crypt.{ Decryptor, EncryptionInfo }
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.xssf.usermodel.{ XSSFSheet, XSSFWorkbook }
import org.apache.poi.EncryptedDocumentException

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Cell, Row }

/**
 * Created by harrison on 02/03/17.
 */

object FileImport extends App {
  var logger = Logger("FileImport")
  logger.info("Received arguments " + args.toList.toString())
  if (args.length <= 5) {
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
    val sheet: XSSFSheet = myWorkbook.getSheetAt(0)
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

  //TODO : Refactor this workaround
  def initLogger : Unit = {
    logger = Logger("FileImport")
  }
}
