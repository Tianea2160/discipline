package org.project.discipline.domain.checklist.repository

import org.project.discipline.domain.checklist.entity.ChecklistEntity
import org.project.discipline.domain.checklist.entity.ChecklistStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface ChecklistRepository : JpaRepository<ChecklistEntity, Long> {
    
    // 사용자별 체크리스트 조회
    fun findByUserIdOrderByCreatedAtDesc(userId: String, pageable: Pageable): Page<ChecklistEntity>
    
    // 사용자별 특정 날짜 체크리스트 조회
    fun findByUserIdAndTargetDateOrderByCreatedAtDesc(userId: String, targetDate: LocalDate): List<ChecklistEntity>
    
    // 상태별 체크리스트 조회
    fun findByStatusOrderByCreatedAtDesc(status: ChecklistStatus, pageable: Pageable): Page<ChecklistEntity>
    
    // 특정 기간 내 체크리스트 조회
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime, 
        endDate: LocalDateTime, 
        pageable: Pageable
    ): Page<ChecklistEntity>
    
    // 사용자별 일일 체크리스트 수 조회
    fun countByUserIdAndCreatedAtAfter(userId: String, after: LocalDateTime): Long
    
    // 사용자별 통계
    @Query("""
        SELECT 
            COUNT(*) as totalChecklists,
            COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCount,
            COUNT(CASE WHEN c.status = 'FAILED' THEN 1 END) as failedCount
        FROM ChecklistEntity c 
        WHERE c.userId = :userId
    """)
    fun getUserStatistics(@Param("userId") userId: String): Map<String, Any>
    
    // 일별 통계
    @Query("""
        SELECT 
            DATE(c.createdAt) as date,
            COUNT(*) as checklistCount,
            COUNT(DISTINCT c.userId) as uniqueUsers,
            COUNT(CASE WHEN c.status = 'COMPLETED' THEN 1 END) as completedCount
        FROM ChecklistEntity c 
        WHERE c.createdAt >= :startDate
        GROUP BY DATE(c.createdAt)
        ORDER BY DATE(c.createdAt) DESC
    """)
    fun getDailyStatistics(@Param("startDate") startDate: LocalDateTime): List<Map<String, Any>>
} 