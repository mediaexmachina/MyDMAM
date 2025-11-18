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

import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

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
import lombok.Setter;
import lombok.ToString;

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
	@ManyToOne(fetch = LAZY, cascade = REMOVE, optional = false)
	private FileEntity file;

	@NotBlank
	@Setter
	@Column(length = 32, name = "task_context")
	private String taskContext;

	@NotBlank
	@Column(length = 32, name = "pending_task")
	private String pendingTask;

	@NotNull
	@Column(name = "updated")
	private Timestamp updated;

	@NotBlank
	@Setter
	@Column(length = 128, name = "worker_host")
	private String workerHost;

	@NotNull
	@Setter
	@Column(name = "worker_pid")
	private Long workerPid;

	/**
	 * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
	 */
	public PendingActivityEntity() {
		// ONLY SET FOR HIBERNATE
	}

	public PendingActivityEntity(final FileEntity file,
								 final String taskContext,
								 final String pendingTask,
								 final String host,
								 final long pid) {
		createDate = new Timestamp(System.currentTimeMillis());
		updated = createDate;
		this.file = file;
		this.taskContext = taskContext;
		this.pendingTask = pendingTask;
		workerHost = host;
		workerPid = pid;
	}

	public PendingActivityEntity setPendingTask(final String pendingTask, final String host, final long pid) {
		this.pendingTask = pendingTask;
		workerHost = host;
		workerPid = pid;
		updated.setTime(System.currentTimeMillis());
		return this;
	}

}
