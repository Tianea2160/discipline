package org.project.discipline.domain.checklist.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RecommendCheckListRequest(
    @field:NotBlank(message = "목표는 필수입니다")
    @field:Size(max = 1000, message = "목표는 1000자 이내로 입력해주세요")
    @JsonProperty("goal")
    val goal: String
)