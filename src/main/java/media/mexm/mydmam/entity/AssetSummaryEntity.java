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

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = AssetSummaryEntity.TABLE_NAME,
	   indexes = {
				   @Index(columnList = "mime_type", name = AssetSummaryEntity.TABLE_NAME + "_mime_type_idx"),
				   @Index(columnList = "file_id", name = AssetSummaryEntity.TABLE_NAME + "_file_id_idx")
	   })
@Getter
@ToString
public class AssetSummaryEntity {

	public static final String TABLE_NAME = "asset_summary";

	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Integer id;

	@NotNull
	@Column(name = "create_date", updatable = false)
	private Timestamp createDate;

	@NotNull
	@JoinColumn(name = "file_id", updatable = false)
	@OneToOne(fetch = LAZY, cascade = DETACH, optional = false)
	private FileEntity file;

	@Column(length = 128, name = "mime_type")
	@Setter
	private String mimeType;

	@Column(length = 2048, name = "specifications")
	@Setter
	private String specifications;

	/**
	 * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
	 */
	public AssetSummaryEntity() {
		// ONLY SET FOR HIBERNATE
	}

	public AssetSummaryEntity(@NotNull final FileEntity file) {
		this.file = file;
		createDate = new Timestamp(System.currentTimeMillis());
	}

}
