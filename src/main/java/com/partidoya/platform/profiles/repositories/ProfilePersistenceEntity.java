package com.partidoya.platform.profiles.repositories;

import com.partidoya.platform.shared.models.AuditableAbstractPersistenceEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class ProfilePersistenceEntity extends AuditableAbstractPersistenceEntity {
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "primary_sport", length = 50)
    private String primarySport;

    @Column(name = "skill_level", length = 20)
    private String skillLevel;

    @Column(length = 80)
    private String district;

    @Column(name = "business_name", length = 140)
    private String businessName;

    @Column(name = "business_phone", length = 20)
    private String businessPhone;

    @Column(name = "operation_district", length = 80)
    private String operationDistrict;

    @ElementCollection
    @CollectionTable(name = "profile_availability", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "time_slot", length = 20)
    private Set<String> availability = new HashSet<>();

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SportPreferencePersistenceEntity> sportPreferences = new ArrayList<>();
}
