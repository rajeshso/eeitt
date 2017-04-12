package uk.gov.hmrc.eeitt.deltaAutomation

import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.transform.FileTransformation

object FileTransformationManual extends FileTransformation with App {

  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation, manualImplementation)

  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }
}