package uk.gov.hmrc.eeitt.deltaAutomation.transform

import java.io.{ File, FileWriter, PrintWriter }

import com.typesafe.scalalogging.Logger
import org.apache.poi.ss.usermodel.{ Row, Sheet, Workbook }
import uk.gov.hmrc.eeitt.deltaAutomation.extract.GMailService

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.language.implicitConversions

trait Writer {

  def locations: Locations
  def logger: Logger

  protected def write(
    goodRowsList: List[RowString],
    badRowsList: List[RowString],
    ignoredRowsList: List[RowString],
    file: File,
    user: User,
    isGoodData: (String, User) => Boolean
  ): Unit = {
    writeRows(s"${locations.badFileLocation}/Ignored${file.getName.replaceFirst("\\.[^.]+$", ".txt")}", ignoredRowsList, "Ignored Rows")
    writeRows(s"${locations.badFileLocation}/${file.getName.replaceFirst("\\.[^.]+$", ".txt")}", badRowsList, "Incorrect Rows ")
    writeRows(s"${locations.outputFileLocation}/${file.getName.replaceFirst("\\.[^.]+$", ".txt")}", goodRowsList, "Correct Rows ")
    if (isGoodData(s"${locations.outputFileLocation}/${file.getName.replaceFirst("\\.[^.]+$", ".txt")}", user)) {
      writeMaster(s"${locations.masterFileLocation}/Master", goodRowsList, file.getName.replaceFirst("\\.[^.]+$", ".txt"))
    }
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

  def writeMaster(filePath: String, rowStrings: List[RowString], fileName: String): Unit = {
    if (rowStrings.nonEmpty) {
      val isAppend = true
      val regex = "\\s([A-za-z]+)\\s.*(\\d{2})[.](\\d{2})[.]20(\\d{2})[.]".r.unanchored
      val details = fileName match {
        case regex(affinityGroup, one, two, three) => Some((affinityGroup, one + two + three))
        case _ =>
          logger.error("the filename is not in the expected format")
          None
      }
      details match {
        case Some(x) =>
          val file = new FileWriter(filePath + x._1, isAppend)
          file.write(x._2 + "\n")
          rowStrings.foreach(x => file.write(x.content + "\n"))
          Some(file.close())
        case None =>
          logger.error("Failed to write to file")
          GMailService.sendError()
      }
    }
  }

  def writeListsToFile(file: File, lineList: List[RowString], user: User, isGoodData: (String, User) => Boolean): (List[RowString], List[RowString], List[RowString]) = {
    val (goodRowsList, badRowsList, ignoredRowsList): (List[RowString], List[RowString], List[RowString]) = user.partitionUserNonUserAndIgnoredRecords(lineList, user)
    badRowsList match {
      case Nil =>
        write(goodRowsList, badRowsList, ignoredRowsList, file, user, isGoodData)
        logger.debug(s"The file ${file.getAbsoluteFile.toString} is successfully parsed and written to the file")
      case _ =>
        write(List.empty[RowString], badRowsList, ignoredRowsList, file, user, isGoodData)
        logger.info(s"The file ${file.getAbsoluteFile.toString} has incorrect rows. The file is rejected.")
    }
    (goodRowsList, badRowsList, ignoredRowsList)
  }
}

trait Reader {

  def logger: Logger

  def readRows(workBook: Workbook): List[RowString] = {
    val sheet: Sheet = workBook.getSheetAt(0)
    val maxNumOfCells: Short = sheet.getRow(0).getLastCellNum
    val rows: Iterator[Row] = sheet.rowIterator().asScala
    val rowBuffer: ListBuffer[RowString] = ListBuffer.empty[RowString]
    rows.foreach { row =>
      val listOfCells: IndexedSeq[String] = for { cell <- 0 to maxNumOfCells } yield {
        Option(row.getCell(cell)).map(_.toString).getOrElse("")
      }
      rowBuffer += RowString(listOfCells.mkString("|"))
    }
    rowBuffer.toList
  }

  def readDataFromFile(fileLocation: String, user: User): List[String] = {
    logger.debug(s" The File Location is : $fileLocation")
    Source.fromFile(fileLocation).getLines().toList.filter(_.startsWith(user.name))
  }
}

trait IOImplementation extends Reader with Writer {

  def reader: Reader = new Reader {
    val logger: Logger = Logger("validation Reader")
  }
}