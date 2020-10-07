package dev.gitlive.difflib.merge

typealias JoinFunction = (a: List<String>) -> String
typealias ConflictFunction = (a: List<Outcome>) -> Any

object Collater {

  fun collateMerge(mergeResult: List<Outcome>, joinFunction: JoinFunction, conflictHandler: ConflictFunction?): MergeResult {
    if (mergeResult.isEmpty()) {
      return MergeResult(listOf(Resolved(emptyList())), joinFunction);
    } else {
      val mergeResult = combineNonConflicts(mergeResult);
      if (mergeResult.size == 1 && mergeResult[0].isResolved()) {
        return MergeResult(mergeResult, joinFunction);
      } else {
        return MergeResult(mergeResult, joinFunction, MergeResultOptions(
          conflict = true, conflictHandler = conflictHandler
        ))
      }
    }
  }

  fun combineNonConflicts(results: List<Outcome>): List<Outcome> {
    val rs = mutableListOf<Outcome>()

    results.forEach { r ->
      if (rs.isNotEmpty() && rs[rs.size - 1].isResolved() && r.isResolved()) {
        val last = rs[rs.size - 1] as Resolved
        last.combine(r as Resolved)
      } else {
        rs.add(r);
      }
    }

    return rs
  }
}
