package dev.gitlive.difflib.merge

abstract class Outcome(private val hasConflicts: Boolean) {
  fun isResolved() = !this.hasConflicts;
  fun isConflicted() = this.hasConflicts;

  abstract fun apply(fn: JoinFunction): Outcome
}

data class ConflictedOpts(
        val left: List<String>,
        val right: List<String>,
        val base: List<String>
)

class Conflicted(
        val left: List<String>,
        val base: List<String>,
        val right: List<String>
) : Outcome(true) {

  // Special constructor because left/base/right positional params
  // are confusing
  companion object {
    fun create(opts: ConflictedOpts) = Conflicted(opts.left, opts.base, opts.right);

  }

  override fun apply(fn: JoinFunction) = create(ConflictedOpts(
      left = listOf(fn(this.left)),
      base = listOf(fn(this.base)),
      right = listOf(fn(this.right))
    ))
}

class Resolved(var result: List<String>) : Outcome(false) {
//  var combiner: Function

  fun combine(other: Resolved) {
    this.result = this.result + other.result;
  }

  override fun apply(fn: JoinFunction) = Resolved(listOf(fn(this.result)));

}
