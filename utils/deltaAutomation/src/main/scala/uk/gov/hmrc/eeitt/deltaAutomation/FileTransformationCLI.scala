package uk.gov.hmrc.eeitt.deltaAutomation

import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService
import uk.gov.hmrc.eeitt.deltaAutomation.transform.FileTransformation

object FileTransformationCLI extends FileTransformation with App {

  GMailService.onNotification()
  process()

  GMailService.sendResult()

  def reInitLogger(testLogger: Logger): Unit = {
    logger = testLogger
  }
}