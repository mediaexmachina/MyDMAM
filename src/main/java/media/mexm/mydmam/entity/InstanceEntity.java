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
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = InstanceEntity.TABLE_NAME,
       uniqueConstraints = {
                             @UniqueConstraint(columnNames = { "name" },
                                               name = InstanceEntity.TABLE_NAME + "_name_uc") },
       indexes = { @Index(columnList = "name", name = InstanceEntity.TABLE_NAME + "_name_idx") })
@Getter
@ToString
public class InstanceEntity {

    public static final String TABLE_NAME = "instance";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @NotBlank
    @Column(length = 128, name = "name", updatable = false)
    private String name;

    @NotBlank
    @Setter
    @Column(length = 256, name = "host")
    private String host;

    @NotNull
    @Setter
    @Column(name = "last_pid")
    private Long lastPid;

    @NotNull
    @Setter
    @Column(name = "last_start_date")
    private Timestamp lastStartDate;

    @OneToMany(mappedBy = "instance", fetch = LAZY, orphanRemoval = true, cascade = REMOVE)
    private final Set<ExternalExecEntity> externalExecEntities = new HashSet<>();

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public InstanceEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public InstanceEntity(@NotBlank final String name,
                          @NotBlank final String host,
                          @NotNull final long pid,
                          @NotNull final long startDate) {
        this.name = name;
        this.host = host;
        lastPid = pid;
        lastStartDate = new Timestamp(startDate);
        createDate = new Timestamp(System.currentTimeMillis());
    }

}
