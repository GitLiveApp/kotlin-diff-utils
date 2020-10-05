package dev.gitlive.difflib.merge

val defaultJoinFunction: (a: List<String>) -> String = { a -> a.joinToString() }
val defaultSplitFunction: (s: String) -> List<String> = { s -> s.split(Regex("(?<=\n)")) }
val defaultConflictFunction = resolver("<<<<<<< YOUR CHANGES",
                                         "=======",
                                         ">>>>>>> APP AUTHORS CHANGES",
                                         defaultJoinFunction);

data class DiffOptions(
  val splitFunction: (s: String) -> List<String> =  defaultSplitFunction,
  val joinFunction: JoinFunction = defaultJoinFunction,
  val conflictFunction: ConflictFunction = defaultConflictFunction
)

fun merge(
        left: String,
        base: String,
        right: String,
        options: DiffOptions = DiffOptions()
): MergeResult {

    val (splitLeft, splitBase, splitRight) = listOf(left, base, right).map { t -> options.splitFunction(t) }

    val mergeResult = Merger.merge(splitLeft, splitBase, splitRight);
    val collatedMergeResults = Collater.collateMerge(mergeResult, options.joinFunction, options.conflictFunction);

  return collatedMergeResults;
}
