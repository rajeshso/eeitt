package uk.gov.hmrc.eeitt.deltaAutomation.transform

import com.typesafe.scalalogging.Logger

sealed trait User {
  var logger = Logger("User")
  type CellsArray = Array[CellValue]
  val cell: Int
  val name: String
  val mandatorySizeOfCells: Int
  val goodRecordFormatFunction: (CellsArray) => RowString
  val badRecordFormatFunction: (CellsArray) => RowString = (cellsArray: CellsArray) => {
    RowString(s"""${cellsArray.map(a => a.content).mkString("|")}""")
  }

  def getUser(userIdIndicator: CellValue): User = {
    userIdIndicator.content match {
      case BusinessUser.name => BusinessUser
      case AgentUser.name => AgentUser
      case _ => UnsupportedUser
    }
  }

  def partitionUserNonUserAndIgnoredRecords(rowsList: List[RowString], user: User): (List[RowString], List[RowString], List[RowString]) = {
    user match {
      case AgentUser => agent(rowsList)
      case BusinessUser => business(rowsList)
      case _ => (List.empty[RowString], List.empty[RowString], List.empty[RowString])
    }
  }

  private def agent(rowsList: List[RowString]): (List[RowString], List[RowString], List[RowString]) = {
    val rowsListExceptHeader: List[RowString] = rowsList.tail
    val (goodRows, badRows): (List[CellsArray], List[CellsArray]) =
      rowsListExceptHeader.map(rowString =>
        rowString.content.split("\\|")).
        filter(cellArray => cellArray.length > 1).
        map(cellStringArray => cellStringArray.map(cellString => CellValue(cellString))).
        partition(cellArray => !(mandatoryCellsMissing(cellArray) || cellHasSelect(cellArray)))

    val (bRows, iRows): (List[CellsArray], List[CellsArray]) = badRows.partition(cellArray => !cellHasSelect(cellArray))

    val badRowsWithReason: List[CellsArray] = addReason(bRows)
    val ignoredRowsWithReason: List[CellsArray] = addReason(iRows)
    val goodRowsList: List[RowString] = goodRows.map(goodRecordFormatFunction)
    val badRowsList: List[RowString] = badRowsWithReason.map(badRecordFormatFunction)
    val ignoredRowsList: List[RowString] = ignoredRowsWithReason.map(badRecordFormatFunction)
    (goodRowsList, badRowsList, ignoredRowsList)
  }

  private def business(rowsList: List[RowString]): (List[RowString], List[RowString], List[RowString]) = {
    val rowsListExceptHeader: List[RowString] = rowsList.tail
    val (goodRows, badRows): (List[CellsArray], List[CellsArray]) =
      rowsListExceptHeader.map(rowString =>
        rowString.content.split("\\|")).
        filter(cellArray => cellArray.length > 1).
        map(cellStringArray => cellStringArray.map(cellString => CellValue(cellString))).
        partition(cellArray => !(mandatoryCellsMissing(cellArray) || cellHasSelect(cellArray)))

    val (bRows, iRows): (List[CellsArray], List[CellsArray]) = badRows.partition(cellArray => !cellHasSelect(cellArray))

    val badRowsWithReason: List[CellsArray] = addReason(bRows)
    val ignoredRowsWithReason: List[CellsArray] = addReason(iRows)
    val goodRowsList: List[RowString] = goodRows.map(goodRecordFormatFunction)
    val badRowsList: List[RowString] = badRowsWithReason.map(badRecordFormatFunction)
    val ignoredRowsList: List[RowString] = ignoredRowsWithReason.map(badRecordFormatFunction)
    (goodRowsList, badRowsList, ignoredRowsList)
  }

  private def addReason(badRows: List[CellsArray]) = {
    badRows.map {
      case cellsArray if cellHasSelect(cellsArray) => Array(
        CellValue(s"The $cell cell is unselected")
      ) ++ cellsArray
      case cellArray if mandatoryCellsMissing(cellArray) => Array(
        CellValue("The length of the cells should be " + mandatorySizeOfCells +
          " and second & third cells should be filled")
      ) ++
        cellArray
      case cellsArray: CellsArray => Array(CellValue("Unknown error")) ++ cellsArray
    }
  }

  def cellHasSelect(cellsArray: CellsArray): Boolean = cellsArray(cell).content == "select"
  def mandatoryCellsMissing(cellsArray: CellsArray): Boolean = cellsArray.length < mandatorySizeOfCells ||
    cellsArray(1).content.isEmpty ||
    cellsArray(2).content.isEmpty
}

case object BusinessUser extends User {
  override val cell: Int = 3
  override val name: String = "001"
  override val mandatorySizeOfCells: Int = 12
  override val goodRecordFormatFunction: (CellsArray) => RowString = (cellsArray: CellsArray) => {
    RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}""")
  }
}

case object AgentUser extends User {
  override val cell: Int = 5
  override val name: String = "002"
  override val mandatorySizeOfCells: Int = 22
  override val goodRecordFormatFunction: (CellsArray) => RowString = (cellsArray: CellsArray) => {
    RowString(s"""${cellsArray(0).content}|${cellsArray(1).content}|||||||||${cellsArray(10).content}|${cellsArray(11).content}|${cellsArray(12).content}|||||||||${cellsArray(21).content}|${cellsArray(22).content}""")
  }

}

case object UnsupportedUser extends User {
  override val cell: Int = 0
  override val name: String = "***"
  override val mandatorySizeOfCells: Int = 0
  override val goodRecordFormatFunction: CellsArray => RowString = (cellsArray: CellsArray) => RowString("")

  override def partitionUserNonUserAndIgnoredRecords(rowsList: List[RowString], user: User): (List[RowString], List[RowString], List[RowString]) = {
    logger.info("An unrecognised file type has been encountered please see the bad output folder")
    (List[RowString](), List[RowString](), List[RowString]())
  }
}