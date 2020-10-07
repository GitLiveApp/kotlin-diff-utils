package dev.gitlive.difflib.merge

fun resolver(
        leftLabel: String,
        baseLabel: String,
        rightLabel: String,
        joinFunction: JoinFunction
): (List<Outcome>) -> String = { results ->
    val res = results.map { result ->
      if (result.isResolved()) {
        val joined = result.apply(joinFunction) as Resolved
        joined.result.joinToString("\n")
      } else {
          val joined = result.apply(joinFunction) as Conflicted
        listOf("$leftLabel\n",
            joined.left.joinToString("\n"),
            "$baseLabel\n",
            joined.right.joinToString("\n"),
            "$rightLabel\n"
        ).joinToString("")
      }
    }
    res.joinToString("")
}

