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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.entity;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.util.Objects.requireNonNull;

import java.sql.Timestamp;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;

@Entity
@Table(name = PendingActivityEntity.TABLE_NAME,
       indexes = {
                   @Index(columnList = "worker_host", name = PendingActivityEntity.TABLE_NAME + "_worker_host_idx"),
                   @Index(columnList = "updated", name = PendingActivityEntity.TABLE_NAME + "_updated_idx"),
                   @Index(columnList = "file_id", name = PendingActivityEntity.TABLE_NAME + "_file_id_idx")
       })
@Getter
@ToString
public class PendingActivityEntity {

    public static final String TABLE_NAME = "pending_activity";
    public static final int PREVIOUS_HANDLERS_SIZES = 2048;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "create_date", updatable = false)
    private Timestamp createDate;

    @NotNull
    @JoinColumn(name = "file_id", updatable = false)
    @ManyToOne(fetch = LAZY, cascade = DETACH, optional = false)
    private FileEntity file;

    @NotBlank
    @Column(length = 64, name = "handler_name", updatable = false)
    private String handlerName;

    @NotBlank
    @Column(length = 64, name = "event_type", updatable = false)
    private String eventType;

    @NotBlank
    @Column(length = PREVIOUS_HANDLERS_SIZES, name = "previous_handlers")
    private String previousHandlers;

    @NotNull
    @Column(name = "updated")
    private Timestamp updated;

    @Setter
    @JoinColumn(name = "instance_id", updatable = true)
    @ManyToOne(fetch = LAZY, cascade = DETACH, optional = true)
    private InstanceEntity instance;

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public PendingActivityEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public PendingActivityEntity(final ActivityHandler activityHandler,
                                 final ActivityEventType eventType,
                                 final String previousHandlers,
                                 final FileEntity file,
                                 final Optional<InstanceEntity> oInstance) {
        handlerName = requireNonNull(activityHandler).getHandlerName();
        this.eventType = requireNonNull(eventType).name();
        this.previousHandlers = requireNonNull(previousHandlers);
        createDate = new Timestamp(System.currentTimeMillis());
        updated = createDate;
        this.file = requireNonNull(file);
        oInstance.ifPresent(this::setInstance);
    }

}
