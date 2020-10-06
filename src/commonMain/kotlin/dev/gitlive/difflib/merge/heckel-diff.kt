package dev.gitlive.difflib.merge

import kotlin.math.max
import kotlin.math.min

class HeckelDiff(val left: List<String>, val right: List<String>) {

  companion object {
      fun executeDiff(oldTextArray: List<String>, newTextArray: List<String>): TypedOutput {
  //      if (!oldTextArray.push) {
  //        throw( Error('Argument is not an array'));
  //      }

        val diffResult = diff(oldTextArray, newTextArray);

        return HeckelDiffWrapper(oldTextArray, newTextArray, diffResult).convertToTypedOutput();
      }

      fun diff(left: List<String>, right: List<String>): List<ChangeRange> {
        val differ = HeckelDiff(left, right);
        return differ.performDiff();
      }
  }


  fun performDiff(): List<ChangeRange> {
    val uniquePositions = this.identifyUniquePositions().sortedByDescending { (it) -> it }

    val (leftChangePos, rightChangePos) = this.findNextChange()
    var changes = ChangeData(leftChangePos, rightChangePos, mutableListOf());
    uniquePositions.forEach { pos ->
      changes = this.getDifferences(changes, pos);
    }

    return changes.changeRanges;
  }

  fun getDifferences(changeData: ChangeData, uniquePositions: UniquePositions): ChangeData {
    val (leftPos, rightPos) = listOf(changeData.leftChangePos, changeData.rightChangePos);
    val (leftUniqPos, rightUniqPos) = uniquePositions;

    if (leftUniqPos < leftPos || rightUniqPos < rightPos) {
      return changeData;
    } else {
      val (leftLo, leftHi, rightLo, rightHi) = this.findPrevChange(leftPos,
                                                     rightPos,
                                                     leftUniqPos-1,
                                                     rightUniqPos-1);
      val (nextLeftPos,
             nextRightPos) = this.findNextChange(leftUniqPos+1,
                                                 rightUniqPos+1);
      val updatedRanges = this.appendChangeRange(changeData.changeRanges,
                                                   leftLo, leftHi,
                                                   rightLo, rightHi);
      return ChangeData(nextLeftPos, nextRightPos, updatedRanges);
    }
  }

  fun findNextChange(leftStartPos: Int = 0, rightStartPos: Int = 0): UniquePositions {
    val lArr = this.left.drop(leftStartPos)
    val rArr = this.right.drop(rightStartPos)
    val offset = this.mismatchOffset(lArr, rArr);

    return UniquePositions(leftStartPos + offset, rightStartPos + offset);
  }

  fun findPrevChange(leftLo: Int, rightLo: Int, leftHi: Int, rightHi: Int): List<Int> {
    if (leftLo > leftHi || rightLo > rightHi) {
      return listOf(leftLo, leftHi, rightLo, rightHi);
    } else {
      val lArr = this.left.slice(leftLo .. leftHi).reversed()
      val rArr = this.right.slice(rightLo .. rightHi).reversed()

      val offset = this.mismatchOffset(lArr, rArr);
      return listOf(leftLo, leftHi - offset, rightLo, rightHi - offset);
    }
  }

  fun mismatchOffset(lArr: List<String>, rArr: List<String>): Int {
    val max = max(lArr.size, rArr.size);
    for (i in 0 until max) {
      if (i >= lArr.size || i >= rArr.size || lArr[i] != rArr[i]) {
        return i;
      }
    }

    return min(lArr.size, rArr.size);
  }

  fun identifyUniquePositions() : List<UniquePositions> {
    val leftUniques = this.findUnique(this.left);
    val rightUniques = this.findUnique(this.right);
    val leftKeys =  leftUniques.keys.toSet()
    val rightKeys =  rightUniques.keys.toSet()
    val sharedKeys =  leftKeys.filter { k -> rightKeys.contains(k) }.toSet()

    val uniqRanges = sharedKeys.mapIndexed { index, k ->
      UniquePositions(leftUniques.getValue(k), rightUniques.getValue(k))
    }
    return listOf(UniquePositions(this.left.size, this.right.size)) + uniqRanges
  }

  fun findUnique(array: List<String>): Map<String, Int> {
    val flaggedUniques = mutableMapOf<String, UniqueItem>();

    array.forEachIndexed { pos, item ->
      flaggedUniques[item] = UniqueItem(pos, !flaggedUniques.contains(item))
    }

    val uniques = mutableMapOf<String, Int>();
    for((key, value) in flaggedUniques.entries) {
      if (value.unique) {
        uniques[key] = value.pos;
      }
    }

    return uniques;
  }

  // given the calculated bounds of the 2 way diff, create the proper
  // change type and add it to the queue.
  fun appendChangeRange(
          changesRanges: MutableList<ChangeRange>,
          leftLo: Int,
          leftHi: Int,
          rightLo: Int,
          rightHi: Int
  ): MutableList<ChangeRange> {
    if (leftLo <= leftHi && rightLo <= rightHi) {
      // for this change, the bounds are both 'normal'. the beginning
      // of the change is before the end.
      changesRanges.add( ChangeRange(Action.change,
                                         leftLo + 1, leftHi + 1,
                                         rightLo + 1, rightHi + 1));
    } else if (leftLo <= leftHi) {
      changesRanges.add( ChangeRange(Action.remove,
                                         leftLo + 1, leftHi + 1,
                                         rightLo + 1, rightLo));
    } else if (rightLo <= rightHi) {
      changesRanges.add( ChangeRange(Action.add,
                                         leftLo + 1, leftLo,
                                         rightLo + 1, rightHi + 1));
    }

    return changesRanges
  }
}

typealias UniquePositions = Pair<Int, Int>;

data class UniqueItem(val pos: Int, val unique: Boolean)

data class TextNode(val text: String, val low: Int? = null)

data class TypedOutput(val oldText: List<TextNode>, val newText: List<TextNode>)


class HeckelDiffWrapper(
        val oldTextArray: List<String>,
        val newTextArray: List<String>,
        val chunks: List<ChangeRange>
) {
  val oldText: MutableList<TextNode/*|string*/> = mutableListOf()
  val newText: MutableList<TextNode/*|string*/> = mutableListOf()

  fun convertToTypedOutput(): TypedOutput {
    var finalIndexes = IndexTracker(0, 0)
    this.chunks.forEach { chunk ->
      val (oldIteration, newIteration) = this.setTextNodeIndexes(chunk, finalIndexes.oldIndex, finalIndexes.newIndex)
      val (oldIndex, newIndex) = this.appendChanges(chunk, finalIndexes.oldIndex + oldIteration, finalIndexes.newIndex + newIteration)
      finalIndexes = IndexTracker(oldIndex, newIndex)
    }

    this.setTheRemainingTextNodeIndexes(finalIndexes.oldIndex,
                                        finalIndexes.newIndex);
    return TypedOutput(this.oldText, this.newText)
  }

  private fun setTextNodeIndexes(chunk: ChangeRange, oldIndex: Int, newIndex: Int): Pair<Int, Int> {
    var oldIteration = 0;
    while (oldIndex + oldIteration < chunk.leftLo - 1) { // chunk indexes from 1
      this.oldText.add( TextNode(this.oldTextArray[oldIndex + oldIteration],
                                     newIndex + oldIteration));
      oldIteration += 1;
    }

    var newIteration = 0;
    while (newIndex + newIteration < chunk.rightLo - 1) {
      this.newText.add( TextNode(this.newTextArray[newIndex + newIteration],
                                     oldIndex + newIteration));
      newIteration += 1;
    }

    return Pair(oldIteration, newIteration);
  }

  fun appendChanges(chunk: ChangeRange, startOldIndex: Int, startNewIndex: Int): Pair<Int, Int> {
    var oldIndex = startOldIndex
    var newIndex = startNewIndex
    while (oldIndex <= chunk.leftHi - 1) {
      this.oldText.add(TextNode(this.oldTextArray[oldIndex]))
      oldIndex += 1;
    }

    while (newIndex <= chunk.rightHi - 1) {
      this.newText.add(TextNode(this.newTextArray[newIndex]))
      newIndex += 1;
    }
    return Pair(oldIndex, newIndex)
  }

  fun setTheRemainingTextNodeIndexes(oldIndex: Int, newIndex: Int) {
    var iteration = 0;
    while (oldIndex + iteration < this.oldTextArray.size) {
      this.oldText.add( TextNode(this.oldTextArray[oldIndex + iteration],
                                     newIndex + iteration));
      iteration += 1;
    }

    while (newIndex + iteration < this.newTextArray.size) {
      this.newText.add( TextNode(this.newTextArray[newIndex + iteration],
                                     oldIndex + iteration));
      iteration += 1;
    }
  }
}

class IndexTracker(val oldIndex: Int, val newIndex: Int)

enum class Action {
  change,
  add,
  remove
}

data class ChangeRange(val action: Action, val leftLo: Int, val leftHi: Int, val rightLo: Int, val rightHi: Int)

data class ChangeData(val leftChangePos: Int, val rightChangePos: Int, val changeRanges: MutableList<ChangeRange>)