package uk.gov.hmrc.eeitt.deltaAutomation

import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.services.GMailService

object FileImportCLI extends FileImport with App {

  GMailService.onNotification()
  process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation)

  GMailService.sendResult()




  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }
}