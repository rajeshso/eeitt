package uk.gov.hmrc.eeitt.deltaAutomation

import java.io.{ File, PrintWriter }

import com.typesafe.scalalogging.Logger

/**
 * Created by rajesh on 05/04/17.
 */
sealed trait User {
  var logger = Logger("User")
  type CellsArray = Array[CellValue]
  val name: String
  val mandatorySizeOfCells: Int
  val goodRecordFormatFunction: (CellsArray) => RowString
  val badRecordFormatFunction = (cellsArray: CellsArray) => {
    (RowString(s"""${cellsArray.map(a => a.content).mkString("|")}"""))
  }

  //TODO: Move the write to file functionality outside partition functionality - Single Responsiblity Rule
  def partitionUserAndNonUserRecords(
    rowsList: List[RowString],
    outputFileLocation: String,
    badFileLocation: String,
    currentDateTime: String,
    inputFileName: String
  ): Unit = {
    val rowsListExceptHeader: List[RowString] = rowsList.tail
    val (goodRows, badRows): (List[CellsArray], List[CellsArray]) = rowsListExceptHeader.map(rowString =>
      rowString.content.split("\\|")).filter(cellArray =>
      cellArray.length > 1).map(cellStringArray => cellStringArray.map(cellString => CellValue(cellString))).partition(cellArray =>
      !(mandatoryCellsMissing(cellArray) || thirdCellHasSelect(cellArray)))

    val badRowsWithReason: List[CellsArray] = badRows.map(cellsArray => cellsArray match {
      case cellArray if mandatoryCellsMissing(cellArray) => Array(
        CellValue("The length of the cells should be " + mandatorySizeOfCells +
          " and second & third cells should be filled")
      ) ++
        cellArray
      case cellsArray if thirdCellHasSelect(cellsArray) => Array(
        CellValue("The third cell is unselected")
      ) ++ cellsArray
      case cellsArray: CellsArray => Array(CellValue("Unknown error")) ++ cellsArray
    })

    val goodRowsList: List[RowString] = goodRows.map(goodRecordFormatFunction)
    val badRowsList: List[RowString] = badRowsWithReason.map(badRecordFormatFunction)
    val fileName: String = currentDateTime + inputFileName + ".txt"
    new File(outputFileLocation)
    new File(badFileLocation)
    write(outputFileLocation, badFileLocation, goodRowsList, badRowsList, fileName)
    logger.info("Succesful records parsed:" + goodRowsList.length)
    logger.info("Unsuccesful records parsed:" + badRowsList.length)
  }

  def thirdCellHasSelect(cellsArray: CellsArray): Boolean = cellsArray(2).content == "select"

  def mandatoryCellsMissing(cellsArray: CellsArray): Boolean = cellsArray.length < mandatorySizeOfCells ||
    cellsArray(1).content.isEmpty ||
    cellsArray(2).content.isEmpty

  //TODO: Move all the write related functionality outside User abstraction
  protected def write(
    outputFileLocation: String,
    badFileLocation: String,
    goodRowsList: List[RowString],
    badRowsList: List[RowString],
    fileName: String
  ): Unit = {
    writeRows(s"$badFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", badRowsList, "Incorrect Rows ")
    writeRows(s"$outputFileLocation/${fileName.replaceFirst("\\.[^.]+$", ".txt")}", goodRowsList, "Correct Rows ")
  }

  private def writeRows(file: String, rowStrings: List[RowString], label: String) = {
    if (rowStrings.size != 0) writeToFile(new File(file), label)({ printWriter => rowStrings.foreach(rowString => (printWriter.println(rowString.content))) })
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
}

case object BusinessUser extends User {
  override val name: String = "001"
  override val mandatorySizeOfCells: Int = 12
  override val goodRecordFormatFunction = (cellsArray: CellsArray) => {
    (RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}"""))
  }
}

case object AgentUser extends User {
  override val name: String = "002"
  override val mandatorySizeOfCells: Int = 23
  override val goodRecordFormatFunction = (cellsArray: CellsArray) => {
    (RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}|${cellsArray(12).content}|||||||||${cellsArray(21).content}|${cellsArray(22).content}"""))
  }
}

case object UnsupportedUser extends User {
  override val name: String = "***"
  override val mandatorySizeOfCells: Int = 0
  override val goodRecordFormatFunction = (cellsArray: CellsArray) => RowString("")

  override def partitionUserAndNonUserRecords(
    fileString: List[RowString],
    outputFileLocation: String,
    badFileLocation: String,
    currentDateTime: String,
    inputFileName: String
  ): Unit = {
    logger.info("An unrecognised file type has been encountered please see the bad output folder")
  }
}