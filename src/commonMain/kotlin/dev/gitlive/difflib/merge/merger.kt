package dev.gitlive.difflib.merge

class Merger(left: List<String>, base: List<String>, right: List<String>) {

  companion object {
    fun merge(left: List<String>, base: List<String>, right: List<String>): List<Outcome> {
      val merger = Merger(left, base, right);
      merger.executeThreeWayMerge();

      return merger.result;
    }
  }

  val result = mutableListOf<Outcome>()
  val text3 = Text3(left, base, right)
  
  fun executeThreeWayMerge() {
    val differences = Diff3.executeDiff(this.text3.left, this.text3.base, this.text3.right)
    var index = 1

    differences.forEach { difference ->
      val initialText = mutableListOf<String>()

      for(lineno in index until difference.baseLo) {
        initialText.add(this.text3.base[lineno - 1])
      }

      if (initialText.isNotEmpty()) {
        this.result.add( Resolved(initialText))
      }

      this.interpretChunk(difference)
      index = difference.baseHi + 1
    }


    val endingText = this.accumulateLines(index, this.text3.base.size, this.text3.base)
    if (endingText.isNotEmpty()) {
      this.result.add(Resolved(endingText))
    }
  }

  fun setConflict(difference: Difference) {
    val conflict = Conflicted.create(ConflictedOpts(
      left = this.accumulateLines(difference.leftLo, difference.leftHi, this.text3.left),
      base = this.accumulateLines(difference.baseLo, difference.baseHi, this.text3.base),
      right = this.accumulateLines(difference.rightLo, difference.rightHi, this.text3.right)
    ));
    this.result.add(conflict);
  }

  fun determineConflict(d: List<ChangeRange>, left: List<String>, right: List<String>) {
    var ia = 1;
    d.forEach { changeRange ->
      for(lineno in ia .. changeRange.leftLo) {
        this.result.add( Resolved(this.accumulateLines(ia, lineno, right)));
      }

      val outcome = this.determineOutcome(changeRange, left, right);
      ia = changeRange.rightHi + 1;
      if (outcome != null) {
        this.result.add(outcome);
      }
    }

    val finalText = this.accumulateLines(ia, right.size + 1, right);
    if (finalText.isNotEmpty()) {
      this.result.add( Resolved(finalText));
    }
  }

  fun determineOutcome(changeRange: ChangeRange, left: List<String>, right: List<String>) : Outcome? {
    if (changeRange.action === Action.change) {
      return Conflicted.create(ConflictedOpts(
        left = this.accumulateLines(changeRange.rightLo, changeRange.rightHi, left),
        right = this.accumulateLines(changeRange.leftLo, changeRange.leftHi, right),
        base = emptyList()
      ));
    } else if (changeRange.action === Action.add) {
      return  Resolved(this.accumulateLines(changeRange.rightLo,
                                               changeRange.rightHi,
                                               left));
    } else {
      return null;
    }
  }

  fun setText(origText: List<String>, lo: Int, hi: Int): MutableList<String> {
    val text = mutableListOf<String>();
    for(i in lo .. hi) {
      text.add(origText[i - 1]);
    }

    return text;
  }

  fun _conflictRange(difference: Difference) {
    val right = this.setText(this.text3.right,
                               difference.rightLo,
                               difference.rightHi);
    val left = this.setText(this.text3.left,
                              difference.leftLo,
                              difference.leftHi);
    val d = HeckelDiff.diff(right, left);
    if ((this._assocRange(d, Action.change) != null || this._assocRange(d, Action.remove) != null) && difference.baseLo <= difference.baseHi) {
      this.setConflict(difference);
    } else {
      this.determineConflict(d, left, right);
    }
  }

  fun interpretChunk(difference: Difference) {
    if (difference.changeType == ChangeType.chooseLeft) {
      val tempText = this.accumulateLines(difference.leftLo,
                                            difference.leftHi,
                                            this.text3.left);
      if (tempText.isNotEmpty()) {
        this.result.add( Resolved(tempText));
      }
    } else if (difference.changeType !== ChangeType.possibleConflict) {
      val tempText = this.accumulateLines(difference.rightLo,
                                            difference.rightHi,
                                            this.text3.right);
      if (tempText.isNotEmpty()) {
        this.result.add( Resolved(tempText));
      }
    } else {
      this._conflictRange(difference);
    }
  }

  fun _assocRange(diff: List<ChangeRange>, action: Action): ChangeRange? {
    for(element in diff) {
      val d = element;
      if (d.action == action) {
        return d;
      }
    }

    return null;
  }

  fun accumulateLines(lo: Int, hi: Int, text: List<String>): List<String> {
    val lines = mutableListOf<String>();
    for(lineno in lo..hi) {
      if (text[lineno-1].isNotEmpty()) {
        lines.add(text[lineno-1]);
      }
    }
    return lines;
  }
}

data class Text3(val left: List<String>,
              val base: List<String>,
              val right: List<String>) 
