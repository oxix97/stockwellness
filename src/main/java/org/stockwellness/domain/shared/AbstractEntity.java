package org.stockwellness.domain.shared;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @CreatedDate
    @DateTimeFormat(iso = DATE_TIME)
    @Column(nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @DateTimeFormat(iso = DATE_TIME)
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
