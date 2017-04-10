package uk.gov.hmrc.eeitt.deltaAutomation

import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService
import uk.gov.hmrc.eeitt.deltaAutomation.transform.FileTransformation

object FileTransformationCLI extends FileTransformation with App {

  GMailService.onNotification()
  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  validateInput(inputFileLocation, outputFileLocation, badFileLocation, inputFileArchiveLocation)
  process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation)

  GMailService.sendResult()

  /*def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }*/
}