package uk.gov.hmrc.eeitt.deltaAutomation

import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService._
import uk.gov.hmrc.eeitt.deltaAutomation.transform.FileTransformation
object FileTransformationAutomated extends FileTransformation with App {

  override val isAutomated: Boolean = true
  onNotification()
  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  process(currentDateTime, inputFileLocation, inputFileArchiveLocation, outputFileLocation, badFileLocation)
}
