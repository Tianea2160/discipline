package org.project.discipline.domain.checklist.dto

data class RecommendCheckListItem(
    val task: String,
    val description: String,
    val priority: Priority = Priority.MEDIUM,
    val estimatedDuration: Long
)

enum class Priority {
    HIGH, MEDIUM, LOW
} 