package minesweeper

import kotlin.random.Random
import minesweeper.CellType.*
import java.lang.Exception
import kotlin.system.exitProcess

/* constants (field dimensions and symbols) */
private const val rows: Int = 9
private const val cols: Int = 9
private const val gridSize: Int = rows * cols
private const val marked: Char = '*'
private const val unexplored: Char = '.'


/* Top level functions to transform index to coords and viceversa */
internal fun indexToCoord(index: Int): Pair<Int, Int> = Pair(index % cols, index / cols)
internal fun coordToIndex(x: Int, y: Int): Int = y * cols + x

/* enumeration for Cell types */
enum class CellType(val char: Char) {
    ZERO('/'),
    ONE('1'),
    TWO('2'),
    THREE('3'),
    FOUR('4'),
    FIVE('5'),
    SIX('6'),
    SEVEN('7'),
    EIGHT('8'),
    MINE('X');
}

data class Cell(val field: Field, val x: Int, val y: Int, var value: CellType) {
    val index: Int
        get() { return coordToIndex(x, y) }
    val neighbours: List<Cell>
        get() {
            return listOfNotNull(
                this.field.findCell(x - 1, y - 1),
                this.field.findCell(x, y - 1),
                this.field.findCell(x + 1, y - 1),
                this.field.findCell(x - 1, y),
                this.field.findCell(x + 1, y),
                this.field.findCell(x - 1, y + 1),
                this.field.findCell(x, y + 1),
                this.field.findCell(x + 1, y + 1)
            )
        }
    val isMined: Boolean
        get() { return value == MINE }
    val isSafe: Boolean
        get() { return !isMined }
    var isExplored: Boolean = false
    var isMarked: Boolean = false

    /*secondary constructors delegates to primary transforming coordinates */
    constructor(field: Field, coordinate: Pair<Int, Int>, value: CellType) :
            this(field, coordinate.first, coordinate.second, value)
    constructor(field: Field, coordinate: List<Int>, value: CellType) :
            this(field, coordinate[0], coordinate[1], value)
    constructor(field: Field, index: Int, value: CellType) :
            this(field, indexToCoord(index), value)

    override fun toString(): String {
        return value.char.toString()
    }
}

class Field() {

    /* field properties */
    internal var field: MutableList<Cell> = MutableList(gridSize) {Cell(this, it, ZERO)}
    internal var minedCells: MutableSet<Int> = mutableSetOf()
    internal var markedCells: MutableSet<Int> = mutableSetOf()
    internal var exploredCells: MutableSet<Int> = mutableSetOf()
    internal var isSetUp: Boolean = false

    /* cell getters */
    internal fun findCell(index: Int): Cell? = if (index in 0 until gridSize) field[index] else null
    internal fun findCell(x: Int, y: Int) = if(x in 0 until cols
                                            && y in 0 until rows) findCell(coordToIndex(x, y)) else null
    internal fun findCell(coordinate: Pair<Int, Int>) = findCell(coordinate.first, coordinate.second)
    internal fun findCell(coordinate: List<Int>) = findCell(coordinate[0], coordinate[1])

    /* cell setters */
    internal fun setCell(index: Int, value: CellType) { findCell(index)?.value = value }
    internal fun setCell(x: Int, y: Int, value: CellType) { findCell(x ,y)?.value = value }
    internal fun setCell(coordinate: Pair<Int, Int>, value: CellType) { findCell(coordinate)?.value = value }
    internal fun setCell(coordinate: List<Int>, value: CellType) { findCell(coordinate)?.value = value }

    private fun sweepField() {
        field.forEach() { it.value = ZERO }
        minedCells = mutableSetOf()
    }

    fun mineField(startIndex: Int, mines: Int) {
        sweepField()
        while (minedCells.size < mines) {
            val newMinePosition = Random.nextInt(gridSize)
            if(newMinePosition != startIndex)
                minedCells.add(newMinePosition)
        }
        minedCells.forEach { setCell(it, MINE) }
        markCells()
    }

    private fun markCells() {
        field.filter { it.isSafe  }
            .forEach {
                val neighbourMinesCount = it.neighbours.count { it.isMined }
                it.value = CellType.values()[neighbourMinesCount]
        }
    }

    internal fun exploreCell(cell: Cell){
        if(cell.isMarked){
            cell.isMarked = false
            markedCells.remove(cell.index)
        }
        cell.isExplored = true
        exploredCells.add(cell.index)
        if (cell.value == ZERO)
            cell.neighbours.filterNot { it.isExplored  }.forEach { exploreCell(it) }
    }

    fun represent(showMine: Boolean = false): String {
        var fieldString = fieldHeader()
        fieldString += fieldBody(showMine)
        fieldString += fieldFooter()

        return fieldString
    }

    override fun toString(): String = represent(true)
}

/* Extension functions to represent field */
private fun Field.fieldHeader(): String {
    var header = " |"
    for (i in 1..cols)
        header += "$i"
    header += "|\n"
    header += "-|"
    for (i in 1..cols)
        header += "-"
    header += "|\n"

    return header
}
private fun Field.fieldFooter(): String {
    var footer = "-|"
    for (i in 1..cols)
        footer += "-"
    footer += "|\n"

    return footer
}
private fun Field.fieldBody(showMine: Boolean = false) : String{
    var body = ""
    field.forEachIndexed() { idx, it ->
        // columns opening
        if (idx % cols == 0) body += "${idx / cols + 1}|"
        // columns
        if ((showMine && it.isMined))
            body += it
        else if(it.isMarked)
            body += marked
        else if (it.isExplored)
            body += it
        else
            body += unexplored
        // columns closing
        if (idx % cols == cols -1) body += "|\n"
    }
    return body
}

/* Extension functions to check answer*/
fun List<String>.isWellFormed(): Triple<Int, Int, String>? {
    return if ((this.count() == 3)
            && this[0].toIntOrNull() in (1..cols)
            && this[1].toIntOrNull() in (1..rows)
            && this[2] in listOf("mine", "free"))
            Triple(this[0].toInt(), this[1].toInt(), this[2])
        else
            null
}


fun main() {
    val field = Field()
    print("How many mines do you want on the field?")
    val mines: Int = readln().trim().toInt()

    do {
        // represent empty field
        println(field.represent(false))

        // asks for command
        var answer: Triple<Int, Int, String>? = null
        while (answer == null) {
            println("Set/unset mine marks or claim a cell as free:")
            answer = readln().split(regex = "\\s+".toRegex()).isWellFormed()
        }
        val cellIndex = coordToIndex(answer.first - 1, answer.second - 1)
        val command = answer.third

        // mines the filed
        if (!field.isSetUp) {
            field.mineField(cellIndex, mines)
            field.isSetUp = true
        }

        // game progression logic
        val cell = field.findCell(cellIndex)
        if(cell != null && !cell.isExplored)
            when{
                command == "free" && cell.isMined -> {
                    field.represent(true)
                    println("You stepped on a mine and failed!")
                    exitProcess(0)
                }
                command == "free" && !cell.isMined -> {
                    if(cell.isMarked)
                        field.markedCells.remove(cellIndex)
                    field.exploreCell(cell)
                }
                command == "mine" && cell.isMarked -> {
                    cell.isMarked = false
                    field.markedCells.remove(cellIndex)
                }
                command == "mine" && !cell.isMarked -> {
                    cell.isMarked = true
                    field.markedCells.add(cellIndex)
                }
                else -> {throw Exception("This should never happen, check code at line 233")}
            }
    } while (field.markedCells != field.minedCells || field.exploredCells.size != gridSize - mines)
    println("Congratulations! You found all the mines!")
}
