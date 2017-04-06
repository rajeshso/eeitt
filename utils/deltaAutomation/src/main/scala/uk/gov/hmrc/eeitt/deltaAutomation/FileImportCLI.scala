package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.File
import java.nio.file.{ Files, StandardCopyOption }
import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Workbook }

/**
 * Created by rajesh on 06/04/17.
 * FileImport Command Line Interface
 */
object FileImportCLI extends FileImport {
  def main(args: Array[String]): Unit = {
    val currentDateTime: String = getCurrentTimeStamp
    logger.info("File Import utility successfully initialized with Identity " + currentDateTime)

    val conf: Config = ConfigFactory.load();
    val inputFileLocation = conf.getString("location.inputfile.value")
    val inputFileArchiveLocation = conf.getString("location.inputfile.archive.value")
    val outputFileLocation = conf.getString("location.outputfile.value")
    val badFileLocation = conf.getString("location.badfile.value")
    logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
    process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation)
  }

  def process(
    currentDateTime: String,
    inputFileLocation: String,
    inputFileArchiveLocation: String,
    outputFileLocation: String,
    badFileLocation: String
  ) = {
    validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileArchiveLocation)
    val files: List[File] = getListOfFiles(inputFileLocation)
    logger.info(s"The following ${files.size} files will be processed ")
    val filesWithIndex = files.zipWithIndex
    for (file <- filesWithIndex) logger.info((file._2 + 1) + " - " + file._1.getAbsoluteFile.toString)
    for (file <- files if isValidFile(file.getCanonicalPath)) {
      logger.info(s"Parsing ${file.getAbsoluteFile.toString} ...")
      val workbook: Workbook = fileAsWorkbook(file.getCanonicalPath)
      val lineList: List[RowString] = readRows(workbook)
      val linesAndRecordsAsListOfList: List[CellsArray] = lineList.map(line => line.content.split("\\|")).map(strArray => strArray.map(str => CellValue(str)))
      val userIdIndicator: CellValue = linesAndRecordsAsListOfList.tail.head.head
      val user: User = getUser(userIdIndicator)
      user.partitionUserAndNonUserRecords(lineList, outputFileLocation, badFileLocation, currentDateTime, file.getAbsoluteFile.getName)
      Files.move(file.toPath, new File(inputFileArchiveLocation + "//" + file.toPath.getFileName).toPath, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def validateInput(
    inputFileLocation: String,
    outputFileLocation: String,
    badFileLocation: String,
    inputFileArchiveLocation: String
  ) = {
    if (!isValidFileLocation(inputFileLocation, true, false)) System.exit(0)
    else if (!isValidFileLocation(outputFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(badFileLocation, false, true)) System.exit(0)
    else if (!isValidFileLocation(inputFileArchiveLocation, false, true)) System.exit(0)
  }

  def reInitLogger(testLogger: Logger): Unit = {
    // Mock Logger
    logger = testLogger
  }

}