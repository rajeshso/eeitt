package uk.gov.hmrc.eeitt.deltaAutomation

import uk.gov.hmrc.eeitt.deltaAutomation.transform.FileTransformation

object FileTransformationManual extends FileTransformation with App {

  override val isAutomated: Boolean = false
  logger.info("File Import utility successfully initialized with Identity " + currentDateTime)
  logger.debug(s"Config values are location.inputfile.value = $inputFileLocation, location.inputfile.archive.value= $inputFileArchiveLocation, location.outputfile.value = $outputFileLocation , location.badfile.value=$badFileLocation")
  process()

}