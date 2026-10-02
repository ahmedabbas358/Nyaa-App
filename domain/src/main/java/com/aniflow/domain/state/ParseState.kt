package com.aniflow.domain.state

/**
 * State representing parsing progress and ambiguity level for releases and search raw items.
 */
enum class ParseState {
    Unparsed,
    Parsing,
    Parsed,
    PartiallyParsed,
    Ambiguous,
    Rejected
}
