package com.arjunsports.contentagent.modules.settings;

import com.arjunsports.contentagent.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Centralized, admin-editable application configuration (AI provider,
 * generation defaults, scheduling defaults, storage, integrations).
 * Entity is scaffolded in the Foundation module; CRUD service/controller
 * and the admin UI are built out in the Application Settings module.
 * Secrets bootstrap from environment variables and may be overridden
 * here at runtime; {@link #secret} rows are masked in any admin UI.
 */
@Getter
@Setter
@Entity
@Table(name = "app_settings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 150)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private SettingCategory category;

    @Column(name = "description")
    private String description;

    @Column(name = "is_secret", nullable = false)
    @Builder.Default
    private boolean secret = false;
}
