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

import java.io.File;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

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

@Entity
@Table(name = AssetTextExtractedFileEntity.TABLE_NAME,
       indexes = {
                   @Index(columnList = "file_id", name = AssetTextExtractedFileEntity.TABLE_NAME + "_file_id_idx")
       })
@Getter
@ToString
public class AssetTextExtractedFileEntity implements RelativePathProvider {

    public static final int NAME_LEN = 256;

    public static final String TABLE_NAME = "asset_textextractedfile";

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
    @Column(length = 256, name = "name", updatable = false)
    private String name;

    @NotNull
    @Column(name = "length", updatable = false)
    private Long length;

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public AssetTextExtractedFileEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public AssetTextExtractedFileEntity(@NotNull final FileEntity file,
                                        @NotNull final String name,
                                        @NotNull final long length) {
        this.file = file;
        createDate = new Timestamp(System.currentTimeMillis());
        this.name = name;
        this.length = length;
    }

    public Map<String, Serializable> getAuditTrailPayload(final File renderedFile) {
        return Map.of(
                "file", renderedFile.getAbsolutePath(),
                "length", length,
                "name", name);
    }

    @Override
    public boolean isGzipEncoded() {
        return true;
    }

    @Override
    public String getRenderedFileNamePrefix() {
        return "text-";
    }
}
