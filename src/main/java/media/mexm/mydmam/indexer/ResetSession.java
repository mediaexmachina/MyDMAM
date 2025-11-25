/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.indexer;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.lucene.document.Document;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.tools.FileEntityConsumer;

/**
 * Not thread safe.
 */
@Slf4j
public class ResetSession implements AutoCloseable, FileEntityConsumer {

	private final Function<FileEntity, Document> fileToDocument;
	private final Consumer<List<Document>> documentsSaver;
	private final LinkedBlockingQueue<Document> queue;

	ResetSession(final Function<FileEntity, Document> fileToDocument,
				 final Consumer<List<Document>> documentsSaver,
				 final int batchSize) {
		this.fileToDocument = Objects.requireNonNull(fileToDocument);
		this.documentsSaver = Objects.requireNonNull(documentsSaver);
		queue = new LinkedBlockingQueue<>(batchSize);
		log.debug("Prepare reset session with a batchSize of {}", batchSize);
	}

	@Override
	public void accept(final FileEntity file) {
		if (queue.remainingCapacity() == 0) {
			close();
		}
		log.trace("Add file {}", file);
		queue.add(fileToDocument.apply(file));
	}

	@Override
	public void close() {
		if (queue.isEmpty()) {
			return;
		}
		log.debug("Sync queue ({} items)", queue.size());
		final var list = new ArrayList<Document>(queue.size());
		queue.drainTo(list);
		documentsSaver.accept(unmodifiableList(list));
	}

}
