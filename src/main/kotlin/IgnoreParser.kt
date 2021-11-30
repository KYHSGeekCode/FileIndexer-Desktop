// https://github.com/codemix/gitignore-parser/blob/master/lib/index.js

fun parse(content: String) {
    content.split(System.lineSeparator()).map {
        it.trim()
    }.filter {
        it.isNotEmpty() && it[0] != '#'
    }.fold(listOf(ArrayList(), ArrayList<String>())) { lists, line ->
        val isNegative = line[0] == '!'
        var tline = line
        if (isNegative) {
            tline = tline.substring(1)
        }
        if (tline[0] == '/') {
            tline = tline.substring(1)
        }
        if (isNegative) {
            lists[1].add(tline)
        } else {
            lists[0].add(tline)
        }
        lists
    }.map { list ->
        list.sorted().flatMap { pattern ->
            listOf(prepareRegexPattern(pattern), preparePartialRegex(pattern))
        }.fold( listOf(ArrayList(), ArrayList<String>())){ lists, prepared ->
//            lists[0].add(prepared[0])
//            lists[1].add(prepared[1])
            lists
        }
    }
}

fun preparePartialRegex(pattern: String): String {
    return pattern.split('/').mapIndexed { idx, item ->
        if (idx > 0) {
            "([\\/]?(" + prepareRegexPattern(item) + "\\b|$))"
        } else {
            "(" + prepareRegexPattern(item) + "\\b)"
        }
    }.joinToString("")
}

fun prepareRegexPattern(pattern: String): String {
    return escapeRegex(pattern).replace("**", "(.+)").replace("*", "([^\\/]+)")
}

fun escapeRegex(pattern: String): String {
    return pattern.replace("[-[]/{}()+?.\\^$|]", "\\$&")
}