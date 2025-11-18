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
import lombok.ToString;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;

@Entity
@Table(name = PendingActivityEntity.TABLE_NAME,
	   indexes = {
				   @Index(columnList = "worker_host", name = PendingActivityEntity.TABLE_NAME + "_worker_host_idx"),
				   @Index(columnList = "updated", name = PendingActivityEntity.TABLE_NAME + "_updated_idx")
	   })
@Getter
@ToString
public class PendingActivityEntity {

	public static final String TABLE_NAME = "pending_activity";

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
	@Column(length = 2048, name = "previous_handlers")
	private String previousHandlers;

	@NotNull
	@Column(name = "updated")
	private Timestamp updated;

	@NotBlank
	@Column(length = 128, name = "worker_host")
	private String workerHost;

	@NotNull
	@Column(name = "worker_pid")
	private Long workerPid;

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
								 final String host,
								 final long pid) {
		handlerName = requireNonNull(activityHandler).getHandlerName();
		this.eventType = requireNonNull(eventType).name();
		this.previousHandlers = requireNonNull(previousHandlers);
		createDate = new Timestamp(System.currentTimeMillis());
		updated = createDate;
		this.file = requireNonNull(file);
		workerHost = requireNonNull(host);
		workerPid = pid;
	}

	public PendingActivityEntity reset(final String host, final long pid) {
		workerHost = host;
		workerPid = pid;
		updated.setTime(System.currentTimeMillis());
		return this;
	}

}
