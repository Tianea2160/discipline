package org.project.discipline.domain.checklist.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RecommendCheckListItem(
    @JsonProperty("task")
    val task: String,
    
    @JsonProperty("description")
    val description: String? = null,
    
    @JsonProperty("priority")
    val priority: Priority = Priority.MEDIUM,
    
    @JsonProperty("estimatedTime")
    val estimatedTime: String? = null
)

enum class Priority {
    HIGH, MEDIUM, LOW
} 