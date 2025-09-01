package com.back.back9.global.jpa.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Setter(AccessLevel.PROTECTED)
    Long id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    LocalDateTime modifiedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity)) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }
}


//package com.back.back9.global.jpa.entity
//
//import jakarta.persistence.*
//import org.springframework.data.annotation.CreatedDate
//import org.springframework.data.annotation.LastModifiedDate
//import org.springframework.data.jpa.domain.support.AuditingEntityListener
//import java.time.LocalDateTime
//
//@MappedSuperclass
//@EntityListeners(AuditingEntityListener::class)
//abstract class BaseEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    var id: Long? = null
//        protected set
//
//    @CreatedDate
//    @Column(name = "created_at", updatable = false)
//    var createdAt: LocalDateTime? = null
//        protected set
//
//
//    @LastModifiedDate
//    @Column(name = "modified_at")
//    var modifiedAt: LocalDateTime? = null
//        protected set
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other !is BaseEntity) return false
//        return id != null && id == other.id
//    }
//
//    override fun hashCode(): Int {
//        return id?.hashCode() ?: 0
//    }
//}