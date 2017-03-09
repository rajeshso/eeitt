import java.io.File
import java.util.Calendar

import org.apache.poi.poifs.crypt.{Decryptor, EncryptionInfo}
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


/**
  * Created by harrison on 02/03/17.
  */
object FileImport extends App{
  println(args.toList)
  val fileLocation: String = args.apply(0)
  val password: String = args.apply(1)
  val myWorkbook = importFile(s"$fileLocation", s"$password")
  val fileAsString = convertFileToString(myWorkbook)
  val splitAgentOrBusiness: List[Array[String]] = fileAsString.map(f => f.split("\\|"))
  val agentOrBusinessUser: String = splitAgentOrBusiness.tail.head.head
  val currentDateTime = Calendar.getInstance.getTime


  val filteredFile: List[String] = agentOrBusinessUser match {
    case "002" => filterAgentUser(fileAsString)
    case "001" => filterBusinessUser(fileAsString)
    case _ => null
  }

  val fileName: String = args.apply(2)
  printToFile(new File(s"$fileName$currentDateTime.txt")) { p => filteredFile.foreach(p.println) }

  def filterBusinessUser(fileString: List[String]): List[String] = {
    val deleteFirstLine = fileString.tail
    val splitString = deleteFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
     splitString.foreach(f => println(f.toList))
    val parsedData = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}"""))
    parsedData
  }

  def filterAgentUser(fileString: List[String]): List[String] = {
    val delFirstLine = fileString.tail
    val splitString = delFirstLine.map(f => f.split("\\|")).filter(f => !(f(1) == "" || f(1) == "select"))
    val parsedData = splitString.map(x => (s"""${x(0)}|${x(1)}|||||||||${x(10)}|${x(11)}|${x(12)}|||||||||${x(21)}|${x(22)}"""))
    parsedData
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  def convertFileToString(workBook: XSSFWorkbook): List[String] = {
    val sheet = myWorkbook.getSheetAt(0)
    val maxNumOfCells = sheet.getRow(0).getLastCellNum
    val rows = sheet.rowIterator()
    val rowBuffer: ListBuffer[String] = ListBuffer.empty[String]
    for (row <- rows) {
      val cells = row.cellIterator()
      val listOfCells = for {cell <- 0 to (maxNumOfCells)} yield {
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
    val fs = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val info = new EncryptionInfo(fs)
    val d: Decryptor = Decryptor.getInstance(info)

    if (!d.verifyPassword(s"$password")) {
      println("unable to process document incorrect password")
    }
    val wb: XSSFWorkbook = new XSSFWorkbook(d.getDataStream(fs))

    wb
  }
}
