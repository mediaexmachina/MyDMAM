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
import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = ExternalExecEntity.TABLE_NAME,
       uniqueConstraints = { @UniqueConstraint(columnNames = { "instance_id", "exec_name" },
                                               name = ExternalExecEntity.TABLE_NAME + "_name_uc") })
@Getter
@ToString
public class ExternalExecEntity {

    public static final String TABLE_NAME = "external_exec";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @NotNull
    @JoinColumn(name = "instance_id", updatable = false)
    @ManyToOne(fetch = LAZY, cascade = DETACH, optional = false)
    private InstanceEntity instance;

    @NotBlank
    @Column(length = 64, name = "name", updatable = false)
    private String execName;

    @NotBlank
    @Column(length = 256, name = "exec_path", updatable = false)
    private String execPath;

    @NotNull
    @Column(name = "exec_modified", updatable = false)
    private Timestamp execModified;

    @Setter
    @NotNull
    @Column(name = "exec_length", updatable = false)
    private Long execLength;

    @NotNull
    @Column(name = "exec_crc", updatable = false)
    private Long execCrc;

    @OneToMany(mappedBy = "externalExec", fetch = LAZY, orphanRemoval = true, cascade = REMOVE)
    private final Set<ExternalExecCapabilityEntity> capabilities = new HashSet<>();

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public ExternalExecEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public ExternalExecEntity(@NotNull final InstanceEntity instance,
                              @NotBlank final String execName,
                              @NotBlank final String execPath,
                              final long execModified,
                              final long execLength,
                              final long execCrc) {
        this.instance = instance;
        this.execName = execName;
        this.execPath = execPath;
        this.execModified = new Timestamp(execModified);
        this.execLength = execLength;
        this.execCrc = execCrc;
        createDate = new Timestamp(System.currentTimeMillis());
    }

}
