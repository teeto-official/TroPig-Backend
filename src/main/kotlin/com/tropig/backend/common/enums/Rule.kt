package com.tropig.backend.common.enums

enum class Rule {
    COC,
    FIASCO,
    ;

    companion object {
        fun fromList(rules: String?): List<Rule> {
            return rules?.split(",")
                ?.map { Rule.valueOf(it) }
                ?: emptyList()
        }
    }
}