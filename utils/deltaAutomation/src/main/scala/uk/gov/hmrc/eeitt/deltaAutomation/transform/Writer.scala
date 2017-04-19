package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.{ File, FileWriter, PrintWriter }

import com.typesafe.scalalogging.Logger

trait Writer {

  private val logger = Logger("Writer")

  protected def write(
    outputFileLocation: String,
    badFileLocation: String,
    goodRowsList: List[RowString],
    badRowsList: List[RowString],
    ignoredRowsList: List[RowString],
    fileName: String
  ): Unit = {
    writeRows(s"$badFileLocation/Ignored${fileName.replaceFirst("\\.[^.]+$", ".txt")}", ignoredRowsList, "Ignored Rows")
    writeRows(s"$badFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", badRowsList, "Incorrect Rows ")
    writeRows(s"$outputFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", goodRowsList, "Correct Rows ")
    writeMaster(s"$outputFileLocation/Master/Master", goodRowsList, fileName.replaceFirst("\\.[^.]+$", ".txt"))
  }

  private def writeRows(file: String, rowStrings: List[RowString], label: String) = {
    if (rowStrings.nonEmpty) writeToFile(new File(file), label)({ printWriter => rowStrings.foreach(rowString => printWriter.println(rowString.content)) })
  }

  def writeToFile(f: File, label: String)(op: (PrintWriter) => Unit): Unit = {
    val writer: PrintWriter = new PrintWriter(f)
    try {
      op(writer)
      logger.info(s"The file with $label is " + f.getAbsoluteFile)
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    } finally {
      writer.close()
    }
  }

  private def writeMaster(filePath: String, rowStrings: List[RowString], fileName: String): Unit = {
    if (rowStrings.nonEmpty) {
      val isAppend = true
      val regex = "\\s([A-za-z]+)\\s.*(\\d{2})[.](\\d{2})[.]20(\\d{2})[.]".r.unanchored
      val divider = fileName match {
        case regex(affinityGroup, one, two, three) => (affinityGroup, one + two + three)
        case _ => throw new IllegalArgumentException("The file name is not in the expected format")
      }

      val file = new FileWriter(filePath + divider._1, isAppend)
      file.write(divider._2 + "\n")
      rowStrings.foreach(x => file.write(x.content + "\n"))
      file.close()
    }
  }
}
