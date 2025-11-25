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
package media.mexm.mydmam.indexer;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ResetSessionTest {

	@Mock
	Function<FileEntity, Document> fileToDocument;
	@Mock
	Consumer<List<Document>> documentsSaver;
	@Mock
	FileEntity oneFile;
	@Mock
	Document oneDocument;
	@Captor
	ArgumentCaptor<List<Document>> documentsSaverListCaptor;

	@Fake(min = 10, max = 100)
	int batchSize;

	ResetSession rs;

	@BeforeEach
	void init() {
		when(fileToDocument.apply(oneFile)).thenReturn(oneDocument);
		rs = new ResetSession(fileToDocument, documentsSaver, batchSize);
	}

	@Nested
	class Batchs {

		List<FileEntity> files;

		@Test
		void test_bigBatch() { // NOSONAR S2699
			files = IntStream.range(0, batchSize * 10)
					.mapToObj(_ -> mock(FileEntity.class))
					.toList();
		}

		@Test
		void test_smallBatch() { // NOSONAR S2699
			files = IntStream.range(0, batchSize / 2)
					.mapToObj(_ -> mock(FileEntity.class))
					.toList();
		}

		@AfterEach
		void init() {
			final var documentsFiles = files.stream().collect(toUnmodifiableMap(f -> f, _ -> mock(Document.class)));
			documentsFiles.forEach((file, document) -> when(fileToDocument.apply(file)).thenReturn(document));

			files.forEach(rs::accept);
			rs.close();

			files.forEach(Mockito::verifyNoInteractions);

			documentsFiles.forEach((file, _) -> verify(fileToDocument, times(1)).apply(file));
			verify(documentsSaver, atLeastOnce()).accept(documentsSaverListCaptor.capture());

			final var allSavedDocumements = documentsSaverListCaptor.getAllValues()
					.stream()
					.flatMap(List::stream)
					.toList();
			assertThat(allSavedDocumements).size().isEqualTo(files.size());

			assertThat(documentsSaverListCaptor.getAllValues().stream()
					.map(List::size)
					.filter(s -> s > batchSize)
					.toList()).isEmpty();

			final var allInjectedDocuments = files.stream().map(documentsFiles::get).toList();
			assertThat(allSavedDocumements).isEqualTo(allInjectedDocuments);

			allSavedDocumements.forEach(Mockito::verifyNoInteractions);
		}

	}

	@Test
	void testAccept() {
		rs.accept(oneFile);
		verify(fileToDocument, times(1)).apply(oneFile);
	}

	@Test
	void testEmptyClose() {// NOSONAR S2699
		rs.close();
	}

	@Test
	void testClose() {
		rs.accept(oneFile);
		rs.close();

		verify(fileToDocument, times(1)).apply(oneFile);
		verify(documentsSaver, times(1)).accept(documentsSaverListCaptor.capture());
		assertThat(documentsSaverListCaptor.getValue()).size().isEqualTo(1);
		assertThat(documentsSaverListCaptor.getValue().get(0)).isEqualTo(oneDocument);

		rs.close();
		rs.close();
	}

}
