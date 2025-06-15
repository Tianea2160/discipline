package org.project.discipline.domain.common.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * 생성자/수정자 정보를 포함하는 기본 엔티티 클래스
 * BaseTimeEntity를 확장하여 생성일시/수정일시와 함께 생성자/수정자 정보를 추적합니다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseAuditEntity(
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    protected var createdBy: String = "anonymous",
    
    @LastModifiedBy
    @Column(name = "modified_by", nullable = false)
    protected var modifiedBy: String = "anonymous"
) : BaseTimeEntity() {
    

    
    /**
     * 엔티티가 새로 생성된 것인지 확인
     */
    fun isNew(): Boolean = createdBy == "anonymous"
    
    /**
     * 엔티티가 수정된 적이 있는지 확인
     */
    fun isModified(): Boolean = modifiedBy != createdBy
    
    /**
     * 감사 정보를 문자열로 반환
     */
    fun getAuditInfo(): String {
        return "Created by: $createdBy at $createdAt, " +
               "Modified by: $modifiedBy at $updatedAt"
    }
} 