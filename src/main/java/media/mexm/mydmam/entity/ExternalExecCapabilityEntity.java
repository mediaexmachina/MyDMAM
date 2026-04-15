/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.entity;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static media.mexm.mydmam.tools.CompareTimestamps.roundToTimestamp;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@Entity
@Table(name = ExternalExecCapabilityEntity.TABLE_NAME,
       uniqueConstraints = { @UniqueConstraint(columnNames = { "external_exec_id", "playbook" },
                                               name = ExternalExecCapabilityEntity.TABLE_NAME + "_playbook_uc") })
@Getter
@ToString
public class ExternalExecCapabilityEntity {

    public static final String TABLE_NAME = "external_exec_capability";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @NotNull
    @JoinColumn(name = "external_exec_id", updatable = false)
    @ManyToOne(fetch = LAZY, cascade = DETACH, optional = false)
    private ExternalExecEntity externalExec;

    @NotBlank
    @Column(length = 256, name = "playbook", updatable = false)
    private String playbook;

    @NotNull
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    @Column(name = "pass", columnDefinition = "TINYINT")
    private boolean pass;

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public ExternalExecCapabilityEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public ExternalExecCapabilityEntity(@NotNull final ExternalExecEntity externalExec,
                                        @NotBlank final String playbook,
                                        final boolean pass) {
        this.externalExec = externalExec;
        this.playbook = playbook;
        this.pass = pass;
        createDate = roundToTimestamp(System.currentTimeMillis());
    }

}
