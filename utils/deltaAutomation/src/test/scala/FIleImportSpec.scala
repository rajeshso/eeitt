import java.io.File

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest._

class FIleImportSpec extends FlatSpec with Matchers {

  "filter business user" should "strip the headers from the file and output only the wanted fields of data into the file" in {
    val businessUserData: List[String] = List("File Type|Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|", "001|XPGD0000010088|ZGD|Gaming Duty (GD)|7.0|Limited|LTD||||BN12 4XL|GB|")
    val parsedBusinessUser = FileImport.filterBusinessUser(businessUserData)
    parsedBusinessUser should be(List("001|XPGD0000010088|||||||||BN12 4XL|GB"))
  }

  "filter agent user" should "strip the headers from the file and output only the wanted fields of data into the file" in {
    val agentData: List[String] = List("File Type|Agent Reference Number|Agent Identification Type|Agent Identification Type Description|Agent Organisation Type|Agent Organisation Type Description|Agent Organisation Name|Agent Title|Agent First Name|Agent Second name|Agent Postal code|Agent Country Code|Customer Registration Number|Tax Regime|Tax Regime Description|Organisation Type|Organisation Type Description|Organisation Name|Customer Title|Customer First Name|Customer Second Name|Customer Postal Code|Customer Country Code|", "002|ZARN0000627|ARN|Agent Reference Number|7.0|Limited Company|TRAVEL MARKETING INTERNATIONAL LTD||||BN12 4XL|GB|XAAP00000000007|ZAPD|Air Passenger Duty (APD)|7.0|Limited Company|Airlines|||||non|")
    val parsedAgentData = FileImport.filterAgentUser(agentData)
    parsedAgentData should be(List("002|ZARN0000627|||||||||BN12 4XL|GB|XAAP00000000007||||||||||non"))
  }

  "import password protected file" should "take a file location and a password and return a XSSF Workbook" in {
    val fileName: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "PASS"
    val path = getClass.getResource(fileName).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.verifyPassword(file.getAbsolutePath, filePassword) shouldBe true
    val workbook: XSSFWorkbook = fileImport.importPasswordVerifiedFile(file.getAbsolutePath, filePassword)
    workbook shouldBe a[XSSFWorkbook]
  }

  "An Incorrect password" should "fail the verification test" in {
    val fileLocation: String = "/TestPasswordProtected.xlsx"
    val filePassword: String = "BlahBlah"
    val path = getClass.getResource(fileLocation).getPath
    val file = new File(path)
    val fileImport = FileImport
    fileImport.initLogger
    val verificationResult: Boolean = fileImport.verifyPassword(file.getAbsolutePath, filePassword)
    verificationResult shouldBe false
  }
  "Convert file to string" should "take an XSSFWorkbook and return a list of strings" in {
    val workbook: XSSFWorkbook = new XSSFWorkbook()
    val workbookAsString: List[String] = FileImport.convertFileToString(workbook)
    workbookAsString shouldBe a [List[_]]
  }
  /*
    "print to file" should "take a java file and a method and create a .txt file" in {
      ???
    }*/
}

