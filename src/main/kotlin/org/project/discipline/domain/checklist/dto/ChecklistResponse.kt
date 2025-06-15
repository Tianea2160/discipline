package org.project.discipline.domain.checklist.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ChecklistResponse(
    @JsonProperty("date")
    val date: LocalDate,
    
    @JsonProperty("goal")
    val goal: String,
    
    @JsonProperty("items")
    val items: List<ChecklistItem>,
    
    @JsonProperty("totalTasks")
    val totalTasks: Int = items.size,
    
    @JsonProperty("estimatedTotalTime")
    val estimatedTotalTime: String? = null
) 