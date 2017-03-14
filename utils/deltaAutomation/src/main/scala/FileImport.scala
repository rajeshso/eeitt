import java.io.File
import java.util.{Calendar, Date}
import java.io.PrintWriter

import org.apache.poi.poifs.crypt.{Decryptor, EncryptionInfo}
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFWorkbook}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{Cell, Row}

/**
  * Created by harrison on 02/03/17.
  */
object FileImport extends App{
  val logger : Logger = Logger("FileImport")
  val fileLocation: String = args.apply(0)
  val password: String = args.apply(1)
  val myWorkbook : XSSFWorkbook = importFile(s"$fileLocation", s"$password")
  val fileAsString : List[String] = convertFileToString(myWorkbook)
  val splitAgentOrBusiness: List[Array[String]] = fileAsString.map(f => f.split("\\|"))
  val agentOrBusinessUser: String = splitAgentOrBusiness.tail.head.head
  val currentDateTime : Date = Calendar.getInstance.getTime
  logger.info("File Import utility initialized at "+ currentDateTime)
  logger.info("Received arguments " + args.toList.toString())

  val filteredFile: List[String] = agentOrBusinessUser match {
    case "002" => filterAgentUser(fileAsString)
    case "001" => filterBusinessUser(fileAsString)
    case _ => null
  }

  val fileName: String = args.apply(2)
  printToFile(new File(s"$fileName$currentDateTime.txt")) { p => filteredFile.foreach(p.println) }

  def filterBusinessUser(fileString: List[String]): List[String] = {
    val deleteFirstLine : List[String] = fileString.tail
    val splitString : List[Array[String]] = deleteFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
    val parsedData : List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}"""))
    parsedData
  }

  def filterAgentUser(fileString: List[String]): List[String] = {
    val delFirstLine : List[String] = fileString.tail
    val splitString : List[Array[String]] = delFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
    val parsedData : List[String] = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}|${x(12)}|||||||||${x(21)}|${x(22)}"""))
    parsedData
  }

  def printToFile(f: File)(op: PrintWriter => Unit) {
    val p : PrintWriter = new PrintWriter(f)
    try {
      op(p)
    }catch {
      case e : Throwable => logger.error(e.getMessage)
    }
    finally {
      logger.info("The output file is "+ f.getAbsoluteFile)
      p.close()
    }
  }

  def convertFileToString(workBook: XSSFWorkbook): List[String] = {
    val sheet : XSSFSheet = myWorkbook.getSheetAt(0)
    val maxNumOfCells : Short = sheet.getRow(0).getLastCellNum
    val rows : Iterator[Row] = sheet.rowIterator()
    val rowBuffer: ListBuffer[String] = ListBuffer.empty[String]
    for (row <- rows) {
      val cells : Iterator[Cell] = row.cellIterator()
      val listOfCells : IndexedSeq[String] = for {cell <- 0 to (maxNumOfCells)} yield {
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

  def importFile(fileLocation: String, password: String): XSSFWorkbook = {
    val fs : NPOIFSFileSystem = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val info : EncryptionInfo = new EncryptionInfo(fs)
    val d: Decryptor = Decryptor.getInstance(info)

    if (!d.verifyPassword(s"$password")) {
      logger.info("unable to process document incorrect password")
    }
    val wb: XSSFWorkbook = new XSSFWorkbook(d.getDataStream(fs))
    wb
  }
}
