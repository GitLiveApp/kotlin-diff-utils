package dev.gitlive.difflib.merge

fun resolver(
        leftLabel: String,
        baseLabel: String,
        rightLabel: String,
        joinFunction: JoinFunction
): (List<Outcome>) -> String = { results ->
    results.map { result ->
      if (result.isResolved()) {
        val joined = result.apply(joinFunction) as Resolved
        return@map joined.result
      } else {
          val joined = result.apply(joinFunction) as Conflicted

        return@map listOf(leftLabel,
            joined.left,
            baseLabel,
            joined.right,
            rightLabel
        ).joinToString("\n");
      }
    }.joinToString("\n");
}

