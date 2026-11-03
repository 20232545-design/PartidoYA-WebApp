package com.partidoya.platform.courts.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "court_media")
@Getter
@Setter
@NoArgsConstructor
public class CourtMediaEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "court_id", nullable = false)
    private Long courtId;
    @Column(nullable = false, length = 30)
    private String source;
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;
    @Column(name = "secure_url", nullable = false, length = 500)
    private String secureUrl;
    @Column(name = "google_photo_name", length = 500)
    private String googlePhotoName;
    @Column(length = 500)
    private String attribution;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "primary_asset", nullable = false)
    private boolean primaryAsset;
}
