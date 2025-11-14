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

import static jakarta.persistence.GenerationType.IDENTITY;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Date;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Entity
@Table(name = FileEntity.TABLE_NAME,
	   indexes = {
				   @Index(columnList = "hash_path", name = FileEntity.TABLE_NAME + "_hash_path_idx"),
				   @Index(columnList = "parent_hash_path", name = FileEntity.TABLE_NAME + "_parent_hash_path_idx"),
				   @Index(columnList = "realm", name = FileEntity.TABLE_NAME + "_realm_idx"),
				   @Index(columnList = "storage", name = FileEntity.TABLE_NAME + "_storage_idx")
	   })
@Getter
@ToString
public class FileEntity {

	public static final int MAX_NAME_SIZE = 64;
	public static final int HASH_STRING_LEN = 64;
	public static final String TABLE_NAME = "file";

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Getter
	private Integer id;

	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(columnDefinition = "TINYINT", name = "directory")
	private boolean directory;

	@NotBlank
	@Column(length = MAX_NAME_SIZE, name = "realm", updatable = false)
	private String realm;

	@NotBlank
	@Column(length = MAX_NAME_SIZE, name = "storage", updatable = false)
	private String storage;

	@NotBlank
	@Column(length = 4096, name = "path", updatable = false)
	private String path;

	@NotBlank
	@Column(length = HASH_STRING_LEN, name = "hash_path", updatable = false)
	private String hashPath;

	@NotBlank
	@Column(length = HASH_STRING_LEN, name = "parent_hash_path", updatable = false)
	private String parentHashPath;

	@Setter
	@NotNull
	@Column(name = "modified")
	private Timestamp modified;

	@Setter
	@NotNull
	@Column(name = "length")
	private Long length;

	@NotNull
	@Column(name = "watch_first", updatable = false)
	private Date watchFirst;

	@Setter
	@NotNull
	@Column(name = "watch_last")
	private Timestamp watchLast;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "watch_marked_as_done", columnDefinition = "TINYINT")
	private boolean watchMarkedAsDone;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "watch_last_is_same", columnDefinition = "TINYINT")
	private boolean watchLastIsSame;

	@Setter
	@NotNull
	@Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
	@Column(name = "watch_done_but_changed", columnDefinition = "TINYINT")
	private boolean watchDoneButChanged;

	/**
	 * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
	 */
	public FileEntity() {
		// ONLY SET FOR HIBERNATE
	}

	public FileEntity(final String realm,
					  final String storage,
					  final CachedFileAttributes firstDetectionFile) {
		watchFirst = new Date();
		this.realm = realm;
		this.storage = storage;
		path = firstDetectionFile.getPath();
		hashPath = hashPath(firstDetectionFile.getPath());
		parentHashPath = hashPath(firstDetectionFile.getParentPath());
		refreshNewFile(firstDetectionFile);
		directory = firstDetectionFile.isDirectory();
		watchLast = new Timestamp(System.currentTimeMillis());
		watchLastIsSame = false;
		watchDoneButChanged = false;
		watchMarkedAsDone = false;
	}

	private String hashPath(final String path) {
		return hashPath(realm, storage, path);
	}

	public static final String hashPath(final String realm, final String storage, final String path) {
		if (realm.contains(":")) {
			throw new IllegalArgumentException("Realm name can't contains \":\" \"" + realm + "\"");
		}
		if (storage.contains(":")) {
			throw new IllegalArgumentException("Storage name can't contains \":\" \"" + storage + "\"");
		}
		if (path.contains(":")) {
			throw new IllegalArgumentException("Full path can't contains \":\" \"" + path + "\"");
		}
		return DigestUtils.sha256Hex(realm + ":" + storage + ":" + path);// NOSONAR S4790
	}

	private void refreshNewFile(final CachedFileAttributes file) {
		modified = new Timestamp(file.lastModified());
		length = file.length();
	}

	public FileEntity update(final CachedFileAttributes seeAgainFile) {
		if (directory) {
			if (watchMarkedAsDone == false) {
				refreshNewFile(seeAgainFile);
			}
		} else {
			watchLastIsSame = modified.getTime() == seeAgainFile.lastModified()
							  && length == seeAgainFile.length();
			if (watchLastIsSame == false) {
				watchLast = new Timestamp(System.currentTimeMillis());
				if (watchMarkedAsDone) {
					watchDoneButChanged = true;
				}
			}
			refreshNewFile(seeAgainFile);
		}
		return this;
	}

	public boolean isTimeQualified(final Duration minFixedStateTime) {
		final var notTooRecent = watchLast.getTime() < System.currentTimeMillis() - minFixedStateTime.toMillis();
		return directory
			   || watchLastIsSame && notTooRecent;
	}

	public FileEntity resetDoneButChanged() {
		watchDoneButChanged = false;
		return this;
	}

	public FileEntity setMarkedAsDone() {
		watchMarkedAsDone = true;
		return this;
	}

	public FileAttributesReference toFileAttributesReference(final boolean exists) {
		return new FileAttributesReference(path, length, modified.getTime(), exists, directory);
	}

}
