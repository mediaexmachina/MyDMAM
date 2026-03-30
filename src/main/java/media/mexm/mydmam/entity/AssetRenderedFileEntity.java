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
import static java.lang.Long.toHexString;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;
import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.dto.RenderedFileResponse;

@Entity
@Table(name = AssetRenderedFileEntity.TABLE_NAME,
       indexes = {
                   @Index(columnList = "file_id", name = AssetRenderedFileEntity.TABLE_NAME + "_file_id_idx")
       })
@Getter
@ToString
public class AssetRenderedFileEntity implements RelativePathProvider {

    public static final String GZIP_ENCODED = "gzip";
    public static final String NOT_ENCODED = "identity";
    public static final int NAME_LEN = 256;

    public static final String TABLE_NAME = "asset_renderedfile";

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
    @Column(length = 128, name = "mime_type", updatable = false)
    private String mimeType;

    @NotBlank
    @Column(length = 128, name = "preview_type", updatable = false)
    private String previewType;

    @NotBlank
    @Column(length = 16, name = "encoded", updatable = false)
    private String encoded;

    @NotNull
    @Column(name = "indexref", updatable = false)
    private Integer indexref;

    @NotBlank
    @Column(length = 256, name = "name", updatable = false)
    private String name;

    @NotNull
    @Column(name = "length", updatable = false)
    private Long length;

    @NotNull
    @Column(name = "etag", updatable = false)
    private Long etag;

    /**
     * NEVER USE DIRECTLY, ONLY SET FOR HIBERNATE
     */
    public AssetRenderedFileEntity() {
        // ONLY SET FOR HIBERNATE
    }

    public AssetRenderedFileEntity(@NotNull final FileEntity file,
                                   @NotNull final DeclaredRenderedFile rendered) {
        this.file = file;
        createDate = new Timestamp(System.currentTimeMillis());
        mimeType = rendered.mimeType();
        previewType = rendered.previewType();
        encoded = rendered.toGzip() ? GZIP_ENCODED : NOT_ENCODED;
        indexref = rendered.index();
        name = rendered.name();
        length = rendered.workingFile().length();

        final var nameb = name.getBytes(UTF_8);
        final var numbers = ByteBuffer.allocate((64 + 32 + 32 + 64) / 8 + nameb.length);
        numbers.putLong(length);
        numbers.putInt(indexref);
        numbers.putInt(file.getId());
        numbers.putLong(createDate.getTime());
        numbers.put(nameb);

        final var crc = new CRC32();
        crc.update(numbers.flip());
        etag = crc.getValue();
    }

    public RenderedFileResponse toRenderedReponse() {
        return new RenderedFileResponse(previewType, name);
    }

    public String getHexETag() {
        return toHexString(etag);
    }

    @Override
    public boolean isGzipEncoded() {
        return GZIP_ENCODED.equals(encoded);
    }

    public Map<String, Serializable> getAuditTrailPayload(final File renderedFile) {
        return Map.of(
                "file", renderedFile.getAbsolutePath(),
                "encoded", encoded,
                "etag", getHexETag(),
                "indexref", indexref,
                "length", length,
                "mimeType", mimeType,
                "name", name,
                "previewType", previewType);
    }

}
