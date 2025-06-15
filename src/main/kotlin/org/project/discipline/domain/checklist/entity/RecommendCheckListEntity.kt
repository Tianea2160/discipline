package org.project.discipline.domain.checklist.entity

import jakarta.persistence.*
import org.hibernate.envers.Audited
import org.project.discipline.domain.common.entity.BaseAuditEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 체크리스트 통합 엔티티
 * 요청과 응답을 하나의 테이블로 관리하며 최소한의 정보만 저장합니다.
 * 
 * @Audited 어노테이션으로 모든 변경 사항이 자동으로 audit 테이블에 기록됩니다.
 */
@Entity
@Table(name = "recommend_check_lists")
@Audited
class RecommendCheckListEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    // 기본 정보
    @Column(name = "user_id")
    val userId: String? = null,
    
    @Column(name = "target_date", nullable = false)
    val targetDate: LocalDate,
    
    @Column(name = "goal", nullable = false, length = 1000)
    val goal: String,
    
    // 응답 정보
    @Column(name = "checklist_json", columnDefinition = "TEXT")
    var checklistJson: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: RecommendCheckListStatus = RecommendCheckListStatus.PENDING,
    
    @Column(name = "error_message", length = 500)
    var errorMessage: String? = null,
    
    // 처리 시간 추적
    @Column(name = "started_at", nullable = false)
    var startedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null
    
) : BaseAuditEntity() {
    
    /**
     * 처리 시작
     */
    fun start() {
        this.startedAt = LocalDateTime.now()
        this.status = RecommendCheckListStatus.PROCESSING
    }
    
    /**
     * 체크리스트 생성 완료 처리
     */
    fun complete(checklistJson: String) {
        this.checklistJson = checklistJson
        this.completedAt = LocalDateTime.now()
        this.status = RecommendCheckListStatus.COMPLETED
        this.errorMessage = null
    }
    
    /**
     * 체크리스트 생성 실패 처리
     */
    fun fail(errorMessage: String) {
        this.errorMessage = errorMessage
        this.completedAt = LocalDateTime.now()
        this.status = RecommendCheckListStatus.FAILED
    }
    
    /**
     * 처리 중 상태로 변경 (deprecated - use start() instead)
     */
    @Deprecated("Use start() instead", ReplaceWith("start()"))
    fun processing() {
        start()
    }
    
    /**
     * 성공 여부 확인
     */
    fun isCompleted(): Boolean = status == RecommendCheckListStatus.COMPLETED
    
    /**
     * 실패 여부 확인
     */
    fun isFailed(): Boolean = status == RecommendCheckListStatus.FAILED
    
    /**
     * 처리 중 여부 확인
     */
    fun isProcessing(): Boolean = status == RecommendCheckListStatus.PROCESSING
    
    /**
     * 처리 시간 계산 (밀리초)
     */
    fun getProcessingTimeMs(): Long? {
        return completedAt?.let { 
            Duration.between(startedAt, it).toMillis()
        }
    }
    
    /**
     * 처리 시간 계산 (초)
     */
    fun getProcessingTimeSeconds(): Double? {
        return getProcessingTimeMs()?.let { it / 1000.0 }
    }
}

/**
 * 체크리스트 상태
 */
enum class RecommendCheckListStatus {
    PENDING,     // 대기 중
    PROCESSING,  // 처리 중
    COMPLETED,   // 완료
    FAILED       // 실패
} 