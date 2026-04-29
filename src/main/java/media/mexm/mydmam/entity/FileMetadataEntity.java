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
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.zip.CRC32;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import media.mexm.mydmam.dto.KeyValueMetadataResponse;

@Entity
@Table(name = FileMetadataEntity.TABLE_NAME,
       indexes = {
                   @Index(columnList = "origin", name = FileMetadataEntity.TABLE_NAME + "_origin_idx"),
                   @Index(columnList = "classifier", name = FileMetadataEntity.TABLE_NAME + "_classifier_idx"),
                   @Index(columnList = "key_name", name = FileMetadataEntity.TABLE_NAME + "_key_name_idx"),
                   @Index(columnList = "entry_crc", name = FileMetadataEntity.TABLE_NAME + "_entry_crc_idx"),
                   @Index(columnList = "file_id", name = FileMetadataEntity.TABLE_NAME + "_file_id_idx")
       })
@Getter
public class FileMetadataEntity {

    public static final String TABLE_NAME = "file_metadata";
    public static final int MAX_VALUE_LENGTH = 1024;
    public static final int MAX_ORIGIN_LENGTH = 32;

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

    @NotNull
    @Column(length = MAX_ORIGIN_LENGTH, updatable = false)
    private String origin;

    @NotNull
    @Column(length = 32, updatable = false)
    private String classifier;

    @NotNull
    @Column(updatable = false)
    private Integer layer;

    @NotNull
    @Column(name = "key_name", length = 128, updatable = false)
    private String key;

    @NotNull
    @Column(name = "key_value", length = MAX_VALUE_LENGTH, updatable = false)
    private String value;

    @NotNull
    @Column(name = "entry_crc", updatable = false)
    private Long entryCrc;

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public FileMetadataEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public FileMetadataEntity(@NotNull final FileEntity file,
                              @NotNull final String origin,
                              @NotNull final String classifier,
                              @NotNull final String key,
                              @NotNull final Integer layer,
                              @NotNull final String value) {
        if (value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Too big value length ("
                                               + value.length() + ") instead of " + MAX_VALUE_LENGTH + ".");
        }
        createDate = new Timestamp(currentTimeMillis());
        this.file = file;
        this.origin = origin;
        this.classifier = classifier;
        this.key = key;
        this.layer = layer;
        this.value = value;

        final var crc = new CRC32();
        crc.update((classifier + ":" + key + "[" + layer + "]").getBytes(UTF_8));
        entryCrc = crc.getValue();
    }

    public KeyValueMetadataResponse toKeyValueMetadataResponse() {
        return new KeyValueMetadataResponse(classifier, key, value);
    }

    public Map<String, Serializable> getAuditTrailPayload() {
        return Map.of(
                "classifier", classifier,
                "key", key,
                "layer", layer,
                "origin", origin,
                "value", value);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder();
        sb.append(file.getRealm());
        sb.append("#");
        sb.append(file.getHashPath());
        sb.append(" ");
        sb.append(classifier);
        sb.append(".");
        sb.append(key);
        if (layer != 0) {
            sb.append("[");
            sb.append(layer);
            sb.append("]");
        }
        sb.append("=");
        sb.append(value);
        sb.append(" (by ");
        sb.append(origin);
        sb.append(")");
        return sb.toString();
    }

}
