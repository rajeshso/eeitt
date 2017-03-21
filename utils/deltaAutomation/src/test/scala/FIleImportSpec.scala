
import java.io.{ File, PrintWriter }
import java.nio.file.{ Path, Paths }
import java.util.Calendar

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.IO
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest._
import com.typesafe.scalalogging.Logger

import scala.io.Source
import scala.collection.mutable.ListBuffer

class FIleImportSpec extends FlatSpec with Matchers {


  "filter business user" should "strip the headers from the file and output only the wanted fields of data into the file as well " in {
    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    val outputFileLocation: String = "src/test/"
    val badFileLocation: String = "src/test/"
    val inputFileName: String = "testFile"
    val businessUserData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001|XPGD0000010088|ZGD|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    )
    val parsedBusinessUser = FileImport.BusinessUser.partitionUserAndNonUserRecords(businessUserData, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
    val fileContents = Source.fromFile(outputFileLocation + currentDateTime + inputFileName + ".txt").getLines()
    fileContents.toList should be(List("001|XPGD0000010088|||||||||BN12 4XL|GB"))
    new File(outputFileLocation + currentDateTime + inputFileName + ".txt").delete()
  }

  "filter agent user" should "strip the headers from the file and output only the wanted fields of data into the file" in {

    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    val outputFileLocation: String = "src/test/"
    val badFileLocation: String = "src/test/"
    val inputFileName: String = "testFile"
    val agentData: List[RowString] = List(
      RowString("File Type|Agent Reference Number|Agent Identification Type|Agent Identification Type Description|Agent Organisation Type|Agent Organisation Type Description|Agent Organisation Name|Agent Title|Agent First Name|Agent Second name|Agent Postal code|Agent Country Code|Customer Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("002|ZARN0000627|ARN|Agent Reference Number|7.0|Limited Company|TRAVEL MARKETING INTERNATIONAL LTD||||BN12 4XL|GB|XAAP00000000007|ZAPD|Air Passenger Duty (APD)|7.0|Limited Company|Airlines|||||non|")
    )
    val parsedAgentData = FileImport.AgentUser.partitionUserAndNonUserRecords(agentData, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
    val fileContents = Source.fromFile(outputFileLocation + currentDateTime + inputFileName + ".txt").getLines()
    fileContents.toList should be(List("002|ZARN0000627|||||||||BN12 4XL|GB|XAAP00000000007||||||||||non"))
    new File(outputFileLocation + currentDateTime + inputFileName + ".txt").delete()
  }

  "filter agent user bad records" should "remove bad agent user records" in {
    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    val outputFileLocation: String = "src/test/"
    val badFileLocation: String = "src/test/"
    val inputFileName: String = "testFile"
    val agentData: List[RowString] = List(
      RowString("File Type|Agent Reference Number|Agent Identification Type|Agent Identification Type Description|Agent Organisation Type|Agent Organisation Type Description|Agent Organisation Name|Agent Title|Agent First Name|Agent Second name|Agent Postal code|Agent Country Code|Customer Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("002| |ARN|Agent Reference Number|7.0|Limited Company|TRAVEL MARKETING INTERNATIONAL LTD||||BN12 4XL|GB|XAAP00000000007|ZAPD|Air Passenger Duty (APD)|7.0|Limited Company|Airlines|||||non|")
    )
    val parsedAgentData = FileImport.AgentUser.partitionUserAndNonUserRecords(agentData, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
    val fileContents = Source.fromFile(badFileLocation + currentDateTime + inputFileName + ".txt").getLines()
    fileContents.toList should be(List("002| |||||||||BN12 4XL|GB|XAAP00000000007||||||||||non"))
    new File(badFileLocation + currentDateTime + inputFileName + ".txt").delete()
  }

  "filter business user bad records" should "remove the bad business user records" in {
    val currentDateTime: String = Calendar.getInstance.getTime.toString.replaceAll(" ", "")
    val outputFileLocation: String = "src/test/"
    val badFileLocation: String = "src/test/"
    val inputFileName: String = "testFile"
    val businessData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001| |ZGD|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    )
    val parsedBusinessData = FileImport.BusinessUser.partitionUserAndNonUserRecords(businessData, outputFileLocation, badFileLocation, currentDateTime, inputFileName)
    val fileContents = Source.fromFile(badFileLocation + currentDateTime + inputFileName + ".txt").getLines()
    fileContents.toList should be(List("001| |||||||||BN12 4XL|GB")) //TODO change data to business user data
    new File(badFileLocation + currentDateTime + inputFileName + ".txt").delete()
  }

  "import password protected file" should "take a file location and a password and return a XSSF Workbook" in {
    val fileName: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "PASS"
    val c1 = getClass.getResource(fileName)
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.verifyPassword(file.getAbsolutePath, filePassword) shouldBe true
    val workbook: XSSFWorkbook = fileImport.getPasswordVerifiedFileAsWorkbook(file.getAbsolutePath, filePassword)
    workbook shouldBe a[XSSFWorkbook]
  }

  "An Incorrect password" should "fail the verification test" in {
    val fileName: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "BlahBlah"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    val verificationResult: Boolean = fileImport.verifyPassword(file.getAbsolutePath, filePassword)
    verificationResult shouldBe false
  }

  "A Correct password" should "pass the verification test" in {
    val fileName: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "PASS"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    val verificationResult: Boolean = fileImport.verifyPassword(file.getAbsolutePath, filePassword)
    verificationResult shouldBe true
  }

  "Convert file to string" should "take an XSSFWorkbook and return a list of strings" in {
    val fileName: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "PASS"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    val myWorkbook: XSSFWorkbook = fileImport.getPasswordVerifiedFileAsWorkbook(file.getAbsolutePath, filePassword)
    val workbookAsString = FileImport.readRows(myWorkbook)
    workbookAsString shouldBe a[List[_]]
  }

  "print to file" should "take a java file and create a .txt file" in {
    val fileName: String = "TestOutputFile"
    val file = new File(fileName)
    val writer = new PrintWriter(file)
    val oneToTen: List[Int] = List.range(1, 10)
    FileImport.printToFile(file) { writer => oneToTen.foreach(writer.println) }
    val i = Source.fromFile(fileName).getLines.flatMap { line =>
      line.split(" ").map(_.toInt)
    }.toList
    oneToTen should equal(i)
    file.delete()
  }

  "A valid file location" should "be verified and returned true" in {
    val path = getClass.getResource("").getPath
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFileLocation(path, true, false) shouldBe true
  }
  "An Invalid file location" should "be verified and returned false" in {
    val inValidpath = "//ABC//DEF//GHI"
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFileLocation(inValidpath, true, false) shouldBe false
  }
  "A file with invalid content " should "be verified and returned false" in {
    val fileName: String = "/InvalidContentNonXLSX.xlsx"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFile(file.getAbsolutePath) shouldBe false
  }
  "A directory path" should "not be considered a file, be verified and returned false" in {
    val path = getClass.getResource("").getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFile(file.getAbsolutePath) shouldBe false
  }
  "A valid file with invalid extension " should "be verified and returned false" in {
    val fileName: String = "/ValidFileWithIncorrectExtension.txt"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFile(file.getAbsolutePath) shouldBe false
  }
}

