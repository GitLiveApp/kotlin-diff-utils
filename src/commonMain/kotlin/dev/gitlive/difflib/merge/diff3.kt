package dev.gitlive.difflib.merge

class Diff2Command(val code: Action,
                   val baseLo: Int,
                   val baseHi: Int,
                   val sideLo: Int,
                   val sideHi: Int) {
  
  companion object {
    fun fromChangeRange(changeRange: ChangeRange): Diff2Command {
      return Diff2Command(changeRange.action,
              changeRange.leftLo,
              changeRange.leftHi,
              changeRange.rightLo,
              changeRange.rightHi);
    }
  }
}

class Diff3(val left: List<String>,
            val base: List<String>,
            val right: List<String>) {
  
  companion object {
    fun executeDiff(left: List<String>, base: List<String>, right: List<String>): List<Difference> {
      return Diff3(left, base, right).getDifferences();
    }
  }
  
  fun getDifferences(): List<Difference> {
    val leftDiff = HeckelDiff.diff(this.base, this.left).map { d ->
      Diff2Command.fromChangeRange(d)
    }
    val rightDiff = HeckelDiff.diff(this.base, this.right).map { d ->
     Diff2Command.fromChangeRange(d);
    }
    return this.collapseDifferences( DiffDoubleQueue(leftDiff.toMutableList(), rightDiff.toMutableList()));
  }

  fun collapseDifferences(diffsQueue: DiffDoubleQueue, differences: MutableList<Difference> = mutableListOf()) : MutableList<Difference> {
    if (diffsQueue.isFinished()) {
      return differences;
    } else {
      val resultQueue =  DiffDoubleQueue();
      val initSide = diffsQueue.chooseSide();
      val topDiff = diffsQueue.dequeue()

      resultQueue.enqueue(initSide, topDiff);

      diffsQueue.switchSides();
      this.buildResultQueue(diffsQueue, topDiff.baseHi, resultQueue);

      differences.add(this.determineDifference(resultQueue,
                                                initSide,
                                                diffsQueue.switchSides()));

      return this.collapseDifferences(diffsQueue, differences);
    }
  }

  fun buildResultQueue(diffsQueue: DiffDoubleQueue,
                   prevBaseHi: Int,
                   resultQueue: DiffDoubleQueue) : DiffDoubleQueue {
    if (this.queueIsFinished(diffsQueue.peek(), prevBaseHi)) {
      return resultQueue;
    } else {
      val topDiff = diffsQueue.dequeue();
      resultQueue.enqueue(diffsQueue.currentSide, topDiff);

      if (prevBaseHi < topDiff.baseHi) {
        diffsQueue.switchSides();
        return this.buildResultQueue(diffsQueue, topDiff.baseHi, resultQueue);
      } else {
        return this.buildResultQueue(diffsQueue, prevBaseHi, resultQueue);
      }
    }
  }

  fun queueIsFinished(queue: List<Diff2Command>, prevBaseHi: Int): Boolean {
    return queue.size == 0 || queue[0].baseLo > prevBaseHi + 1;
  }

  fun determineDifference(diffDiffsQueue: DiffDoubleQueue, initSide: Side, finalSide: Side) : Difference {
    val baseLo = diffDiffsQueue.get(initSide)[0].baseLo;
    val finalQueue = diffDiffsQueue.get(finalSide);
    val baseHi = finalQueue[finalQueue.size - 1].baseHi;

    val (leftLo, leftHi) = this.diffableEndpoints(diffDiffsQueue.get(Side.left), baseLo, baseHi);
    val (rightLo, rightHi) = this.diffableEndpoints(diffDiffsQueue.get(Side.right), baseLo, baseHi);

    val leftSubset = this.left.slice(leftLo-1 until  leftHi);
    val rightSubset = this.right.slice(rightLo-1 until rightHi);
    val changeType = this.decideAction(diffDiffsQueue, leftSubset, rightSubset);

    return  Difference(changeType, leftLo, leftHi, rightLo, rightHi, baseLo, baseHi);
  }

  fun diffableEndpoints(commands: List<Diff2Command>, baseLo: Int, baseHi: Int): Pair<Int, Int> {
    if (commands.isNotEmpty()) { //TODO
      val firstCommand = commands[0];
      val lastCommand = commands[commands.size - 1];
      val lo = firstCommand.sideLo - firstCommand.baseLo + baseLo;
      val hi = lastCommand.sideHi  - lastCommand.baseHi  + baseHi;

      return Pair(lo, hi);
    } else {
      return Pair(baseLo, baseHi);
    }
  }

  fun decideAction(diffDiffsQueue: DiffDoubleQueue,
               leftSubset: List<String>,
               rightSubset: List<String>): ChangeType {
    if (diffDiffsQueue.isEmpty(Side.left)) {
      return ChangeType.chooseRight;
    } else if (diffDiffsQueue.isEmpty(Side.right)) {
      return ChangeType.chooseLeft;
    } else {
      // leftSubset deepEquals rightSubset
      if (!leftSubset.withIndex().all { (i, x) -> rightSubset[i] == x }) {
        return ChangeType.possibleConflict;
      } else {
        return ChangeType.noConflictFound;
      }
    }
  }
}

data class Difference(val changeType: ChangeType,
              val leftLo: Int,
              val leftHi: Int,
              val rightLo: Int,
              val rightHi: Int,
              val baseLo: Int,
              val baseHi: Int) 

enum class ChangeType(private val value :String) {
  chooseRight("choose_right"),
  chooseLeft("choose_left"),
  possibleConflict("possible_conflict"),
  noConflictFound("no_conflict_found");

  override fun toString() = value
}

enum class Side {
  left,
  right
}

data class Diffs(val left: MutableList<Diff2Command>, val right: MutableList<Diff2Command>) {
  operator fun get(side: Side) = if(side == Side.left) left else right
}

class DiffDoubleQueue(left: MutableList<Diff2Command> = mutableListOf(), right: MutableList<Diff2Command> = mutableListOf()) {
  lateinit var currentSide: Side
  val diffs = Diffs(left, right)

  fun dequeue(side: Side = this.currentSide): Diff2Command {
    return this.diffs[side].removeAt(0);
  }

  fun peek(side: Side = this.currentSide): MutableList<Diff2Command> {
    return this.diffs[side];
  }

  fun isFinished(): Boolean {
    return this.isEmpty(Side.left) && this.isEmpty(Side.right);
  }

  fun enqueue(side: Side = this.currentSide, value: Diff2Command): Int {
    this.diffs[side].add(value)
    return this.diffs[side].size
  }

  fun get(side: Side = this.currentSide): MutableList<Diff2Command> {
    return this.diffs[side];
  }

  fun isEmpty(side: Side = this.currentSide): Boolean {
    return this.diffs[side].size == 0;
  }

  fun switchSides(side: Side = this.currentSide): Side {
    this.currentSide = if(side == Side.left)  Side.right else Side.left;
    return currentSide
  }

  fun chooseSide(): Side {
    if (this.isEmpty(Side.left)) {
      this.currentSide = Side.right;
    } else if (this.isEmpty(Side.right)) {
      this.currentSide = Side.left;
    } else {
      this.currentSide = if(this.get(Side.left)[0].baseLo <= this.get(Side.right)[0].baseLo) Side.left else Side.right
    }

    return this.currentSide;
  }
}
