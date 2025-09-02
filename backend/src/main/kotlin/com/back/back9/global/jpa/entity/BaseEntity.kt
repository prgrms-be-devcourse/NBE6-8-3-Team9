package com.back.back9.global.jpa.entity

import jakarta.persistence.*
import lombok.AccessLevel
import lombok.Getter
import lombok.Setter
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
@Getter
abstract class BaseEntity {
    @JvmField
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.PROTECTED)
    var id: Long? = null

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    open var createdAt: LocalDateTime? = null

    @JvmField
    @LastModifiedDate
    @Column(name = "modified_at")
    var modifiedAt: LocalDateTime? = null

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is BaseEntity) return false
        val that = o
        return id == that.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }
}
