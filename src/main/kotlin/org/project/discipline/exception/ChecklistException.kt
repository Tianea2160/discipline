package org.project.discipline.exception

class ChecklistGenerationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class InvalidChecklistFormatException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) 