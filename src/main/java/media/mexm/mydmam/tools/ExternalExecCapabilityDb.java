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
package media.mexm.mydmam.tools;

import static java.lang.Math.ceilDiv;
import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

/**
 * Not thread safe!
 */
@Data
public class ExternalExecCapabilityDb {// TODO test

    private int version;
    private HashMap<String, ExecPlaybooks> binaries;
    @JsonIgnore
    private HashMap<String, ExecPlaybooks> checkedBinaries;

    @Data
    public static class ExecPlaybooks {
        private long size;
        private long date;
        private long crc;
        private HashMap<String, Boolean> results;

    }

    private long getCrc(final File exec) {
        try (final var reader = new RandomAccessFile(exec, "r")) {
            final var channel = reader.getChannel();
            final var buff = ByteBuffer.allocateDirect((int) Math.min(100_000l, exec.length()));

            final var crc = new CRC32();
            while (channel.read(buff) > 0) {
                crc.update(buff.flip());
                buff.clear();
            }
            return crc.getValue();
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't read " + exec, e);
        }
    }

    private ExecPlaybooks getMakeExecPlaybooks(final File exec) {
        final var path = exec.getAbsolutePath();

        if (checkedBinaries.containsKey(path)) {
            return checkedBinaries.get(path);
        }

        final var computedDate = ceilDiv(exec.lastModified(), 1000) * 1000;

        if (binaries.containsKey(path)) {
            final var result = binaries.get(path);

            if (result.getDate() == computedDate
                && result.getSize() == exec.length()
                && result.getCrc() == getCrc(exec)) {
                checkedBinaries.put(path, result);
                return result;
            }
        }

        final var result = new ExecPlaybooks();
        result.setDate(computedDate);
        result.setSize(exec.length());
        result.setCrc(getCrc(exec));
        result.setResults(new HashMap<>());

        binaries.put(path, result);
        checkedBinaries.put(path, result);
        return result;
    }

    private void init() {
        if (version == 0) {
            version = 1;
        } else if (version != 1) {
            throw new IllegalArgumentException("Invalid version=" + version + ", expect 1");
        }
        if (binaries == null) {
            binaries = new HashMap<>();
            checkedBinaries = new HashMap<>();
        }
        if (checkedBinaries == null) {
            checkedBinaries = new HashMap<>();
        }
    }

    public Optional<Boolean> getPlaybookResult(final File exec, final String playbook) {
        init();
        return Optional.ofNullable(getMakeExecPlaybooks(exec).getResults().get(playbook));
    }

    public void addPlaybookResult(final File exec, final String playbook, final boolean pass) {
        init();
        getMakeExecPlaybooks(exec).getResults().put(playbook, pass);
    }

    public Map<String, Boolean> getAllPlaybookResults(final File exec) {
        init();
        return unmodifiableMap(getMakeExecPlaybooks(exec).getResults());
    }

}
