package com.partidoya.platform.profiles.repositories;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sport_preferences")
@Getter
@Setter
@NoArgsConstructor
public class SportPreferencePersistenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sport_name", nullable = false, length = 50)
    private String sportName;

    @Column(name = "skill_level", nullable = false, length = 20)
    private String skillLevel;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfilePersistenceEntity profile;
}
