package com.financer.shared.audit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import java.time.LocalDateTime;

/**
 * Base class for auditable entities providing common audit fields
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class AuditableEntity {
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private String createdBy;
    
    private String updatedBy;
    
    @Version
    private Long version;
    
    /**
     * Sets audit fields for creation
     */
    public void onCreate(String userId) {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = userId != null ? userId : "SYSTEM";
        this.updatedBy = userId != null ? userId : "SYSTEM";
        this.version = 0L;
    }
    
    /**
     * Sets audit fields for updates
     */
    public void onUpdate(String userId) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = userId != null ? userId : "SYSTEM";
    }
}