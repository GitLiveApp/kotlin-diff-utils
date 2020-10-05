package dev.gitlive.difflib.merge

data class MergeResultOptions(
        val conflictHandler: ConflictFunction? = null,
        val conflict: Boolean? = null
)

class MergeResult(
        val results: List<Outcome>,
        val joinFunction: JoinFunction,
        options: MergeResultOptions = MergeResultOptions()
) {
  val conflict: Boolean = options.conflict ?: false
  val conflictHandler: ConflictFunction? = options.conflictHandler

  fun isSuccess() = !this.conflict

  fun isConflict() = this.conflict

  fun joinedResults(): Any /*List<Outcome> | String*/ {
    if (this.isConflict()) {
      if (this.conflictHandler != null) {
        return this.conflictHandler!!(this.results)
      } else {
        return this.results;
      }
    } else {
      val rs = this.results.first() as Resolved
      this.results.drop(1).forEach { r -> rs.combine(r as Resolved) }

      return rs.apply(this.joinFunction).result;
    }
  }
}
