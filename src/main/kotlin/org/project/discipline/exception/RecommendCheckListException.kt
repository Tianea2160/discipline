package org.project.discipline.exception

class RecommendCheckListGenerationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class InvalidRecommendCheckListFormatException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) 