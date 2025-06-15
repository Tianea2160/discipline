package org.project.discipline.domain.checklist.repository

import org.project.discipline.domain.checklist.entity.RecommendCheckListEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.history.RevisionRepository
import org.springframework.stereotype.Repository

@Repository
interface RecommendCheckListRepository :
    JpaRepository<RecommendCheckListEntity, Long>,
    RevisionRepository<RecommendCheckListEntity, Long, Long> {

    // 사용자별 체크리스트 조회
    fun findByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<RecommendCheckListEntity>
}