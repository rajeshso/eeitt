package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.File

import org.apache.poi.poifs.crypt.{ Decryptor, EncryptionInfo }
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

trait WorkBookProcessing {

  val password: String
  val iOImplementation: IOImplementation
  def getFileAsWorkbook(fileLocation: String): XSSFWorkbook = {
    val fs = new NPOIFSFileSystem(new File(s"$fileLocation"), true)
    val info = new EncryptionInfo(fs)
    val d: Decryptor = Decryptor.getInstance(info)

    if (!d.verifyPassword(password)) {
      println("unable to process document incorrect password")
    }
    new XSSFWorkbook(d.getDataStream(fs))
  }

  def getRows(file: File): List[RowString] = {
    val workbook: Workbook = getFileAsWorkbook(file.getCanonicalPath)
    iOImplementation.readRows(workbook)
  }

}
