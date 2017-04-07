package uk.gov.hmrc.eeitt.deltaAutomation

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.Logger

/**
 * Created by rajesh on 06/04/17.
 * FileImport Command Line Interface
 */
object FileImportCLI extends FileImport with App {

  val currentDateTime: String = getCurrentTimeStamp
  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  val conf: Config = ConfigFactory.load()
  val inputFileLocation = conf.getString("location.inputfile.value")
  val inputFileArchiveLocation = conf.getString("location.inputfile.archive.value")
  val outputFileLocation = conf.getString("location.outputfile.value")
  val badFileLocation = conf.getString("location.badfile.value")
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileArchiveLocation)
  process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation)

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