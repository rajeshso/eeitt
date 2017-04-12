package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.{ File, PrintWriter }
import java.nio.file.Files.{ exists, isDirectory, isReadable, isWritable }
import java.nio.file.{ Path, Paths }
import java.util.Calendar

import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.Workbook
import org.scalatest._
import uk.gov.hmrc.eeitt.deltaAutomation.FileTransformationManual

import scala.io.Source

class FileTransformationCLISpec extends FlatSpec with Matchers {

  "filter business user" should "strip the headers from the file and output only the wanted fields of data into the file as well " in {
    val businessUserData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001|XPGD0000010088|ZGD|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = BusinessUser.partitionUserNonUserAndIgnoredRecords(businessUserData)
    goodRowsList(0).content should startWith("001|XPGD0000010088|||||||||BN12 4XL|GB")
  }

  "filter agent user" should "strip the headers from the file and output only the wanted fields of data into the file" in {
    val agentData: List[RowString] = List(
      RowString("File Type|Agent Reference Number|Agent Identification Type|Agent Identification Type Description|Agent Organisation Type|Agent Organisation Type Description|Agent Organisation Name|Agent Title|Agent First Name|Agent Second name|Agent Postal code|Agent Country Code|Customer Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("002|ZARN0000627|ARN|Agent Reference Number|7.0|Limited Company|TRAVEL MARKETING INTERNATIONAL LTD||||BN12 4XL|GB|XAAP00000000007|ZAPD|Air Passenger Duty (APD)|7.0|Limited Company|Airlines|||||non|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = AgentUser.partitionUserNonUserAndIgnoredRecords(agentData)
    goodRowsList(0).content should startWith("002|ZARN0000627|||||||||BN12 4XL|GB|XAAP00000000007||||||||||non")
  }

  "filter agent user bad records" should "remove bad agent user records because the second cell is empty" in {
    val agentData: List[RowString] = List(
      RowString("File Type|Agent Reference Number|Agent Identification Type|Agent Identification Type Description|Agent Organisation Type|Agent Organisation Type Description|Agent Organisation Name|Agent Title|Agent First Name|Agent Second name|Agent Postal code|Agent Country Code|Customer Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("002||ARN|Agent Reference Number|7.0|Limited Company|TRAVEL MARKETING INTERNATIONAL LTD||||BN12 4XL|GB|XAAP00000000007|ZAPD|Air Passenger Duty (APD)|7.0|Limited Company|Airlines|||||non|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = AgentUser.partitionUserNonUserAndIgnoredRecords(agentData)
    badRowsList(0).content should startWith("The length of the cells should be 23 and second & third cells should be filled|")
  }

  "filter business user bad records" should "remove the bad business user records because its second cell is empty" in {
    val businessData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001||ZGD|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = BusinessUser.partitionUserNonUserAndIgnoredRecords(businessData)
    badRowsList(0).content should startWith("The length of the cells should be 12 and second & third cells should be filled")
  }

  "filter business user bad records" should "remove the bad business user records because its third cell continues to be select" in {
    val businessData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001|12345|select|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = BusinessUser.partitionUserNonUserAndIgnoredRecords(businessData)
    ignoredRowsList(0).content should startWith("The third cell is unselected|001|12345|select|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB")
  }

  "filter business user good and bad records" should "filter the bad business user records because its third cell continues to be select, but the good one should pass" in {
    val businessData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001|12345|select|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|"),
      RowString("001|XQBD00000000|BINGO|Bingo Duty (BD)|7|Limited Company|Bingo||||BN12 4XL|GB|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = BusinessUser.partitionUserNonUserAndIgnoredRecords(businessData)
    ignoredRowsList(0).content should startWith("The third cell is unselected|001|12345|select|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB")
    goodRowsList(0).content should startWith("001|XQBD00000000|||||||||BN12 4XL|GB")
    badRowsList.size shouldBe (0)
  }

  "filter business user good and ignored records" should "filter the ignored business user records because its third cell continues to be select, but the good one should pass" in {
    val businessData: List[RowString] = List(
      RowString("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|"),
      RowString("001|XQAL00000100727|ZAGL|Aggregate Levy (AGL)|7|Limited Company|NEEDHAM CHALKS (HAM) LIMITED||||IP6 8EL|GB|"),
      RowString("001||select|select|select|select||||||select|")
    )
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = BusinessUser.partitionUserNonUserAndIgnoredRecords(businessData)
    ignoredRowsList(0).content should startWith("The third cell is unselected|001||select|select|select|select||||||select")
    goodRowsList(0).content should startWith("001|XQAL00000100727|||||||||IP6 8EL|GB")
    badRowsList.size shouldBe (0)
  }

  "Read rows" should "take an XSSFWorkbook and return a list of Rowstring" in {
    val fileName: String = "/validFile.xlsx"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    val myWorkbook: Workbook = FileTransformationManual.getFileAsWorkbook(file.getAbsolutePath)
    val workbookAsString = FileTransformationManual.readRows(myWorkbook)
    workbookAsString shouldBe a[List[_]]
  }

  "print to file" should "take a java file and create a .txt file" in {
    val fileName: String = "TestOutputFile"
    val file = new File(fileName)
    val writer = new PrintWriter(file)
    val oneToTen: List[Int] = List.range(1, 10)
    FileTransformationManual.writeToFile(file, "TestOutputFile")({ writer => oneToTen.foreach(writer.println) })
    val i = Source.fromFile(fileName).getLines.flatMap { line =>
      line.split(" ").map(_.toInt)
    }.toList
    oneToTen should equal(i)
    file.delete()
  }

  "A valid file location" should "be verified and returned true" in {
    val path = getClass.getResource("").getPath
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    isValidFileLocation(path, true, false) shouldBe true
  }

  "An Invalid file location" should "be verified and returned false" in {
    val inValidpath = "//ABC//DEF//GHI"
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    isValidFileLocation(inValidpath, true, false) shouldBe false
  }

  "A directory path" should "not be considered a file, be verified and returned false" in {
    val path = getClass.getResource("").getPath
    val file = new File(path)
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFile(file.getAbsolutePath) shouldBe false
  }

  "A file with invalid content " should "be verified and returned false" in {
    val fileName: String = "/InvalidContentNonXLSX.xlsx"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    fileImport.isValidFile(file.getAbsolutePath) shouldBe false
  }

  "A .xlsx file with valid content" should "be opened and produce a list of strings" in {
    val fileName: String = "/validFile.xlsx"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileTransformationManual
    fileImport.reInitLogger(Logger("TestFileImport"))
    val myWorkbook: Workbook = fileImport.getFileAsWorkbook(file.getAbsolutePath)
    val workbookAsString = FileTransformationManual.readRows(myWorkbook)
    workbookAsString shouldBe a[List[_]]
  }

  def isValidFileLocation(fileLocation: String, read: Boolean, write: Boolean): Boolean = {
    val path: Path = Paths.get(fileLocation)
    if (!exists(path) || !isDirectory(path)) {
      false
    } else if (read && !isReadable(path)) {
      false
    } else if (write && !isWritable(path)) {
      false
    } else {
      true
    }
  }
}

