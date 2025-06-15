package org.project.discipline.domain.checklist.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class RecommendCheckListRequest(
    @field:NotBlank(message = "목표는 필수입니다")
    val goal: String,
    
    val context: String? = null,
    
    val date: LocalDate? = null
) 