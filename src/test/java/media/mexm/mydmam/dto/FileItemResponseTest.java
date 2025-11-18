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
package media.mexm.mydmam.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class FileItemResponseTest {

	@Mock
	FileEntity fEntity;

	@Fake
	String path;
	@Fake
	String name;
	@Fake
	String hashPath;
	@Fake
	long time;
	@Fake
	long length;
	@Fake
	String realm;
	@Fake
	String storage;
	@Fake
	String falseRealm;
	@Fake
	String falseStorage;

	FileItemResponse response;
	boolean directory;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();

		when(fEntity.getRealm()).thenReturn(realm);
		when(fEntity.getStorage()).thenReturn(storage);
		when(fEntity.getPath()).thenReturn("/" + path + "/" + name);
		when(fEntity.getHashPath()).thenReturn(hashPath);
		when(fEntity.getModified()).thenReturn(Timestamp.from(Instant.ofEpochMilli(time)));
		when(fEntity.getLength()).thenReturn(length);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(fEntity);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCreateFromEntity_regularFile(final boolean watchMarkedAsDone) {
		directory = false;
		when(fEntity.isDirectory()).thenReturn(directory);
		when(fEntity.isWatchMarkedAsDone()).thenReturn(watchMarkedAsDone);

		response = FileItemResponse.createFromEntity(fEntity, realm, storage);
		assertEquals(directory, response.directory());
		assertEquals(hashPath, response.hashPath());
		assertEquals(length, response.length());
		assertEquals(time, response.modified());
		assertEquals(name, response.name());
		assertEquals(watchMarkedAsDone == false, response.justDetected());

		checkOk();
		verify(fEntity, atLeastOnce()).getLength();
		verify(fEntity, atLeastOnce()).isWatchMarkedAsDone();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testCreateFromEntity_directory(final boolean watchMarkedAsDone) {
		directory = true;
		when(fEntity.isDirectory()).thenReturn(directory);
		when(fEntity.isWatchMarkedAsDone()).thenReturn(watchMarkedAsDone);

		response = FileItemResponse.createFromEntity(fEntity, realm, storage);
		assertEquals(directory, response.directory());
		assertEquals(hashPath, response.hashPath());
		assertEquals(-1l, response.length());
		assertEquals(time, response.modified());
		assertEquals(name, response.name());
		assertFalse(response.justDetected());

		checkOk();
	}

	private void checkOk() {
		verify(fEntity, atLeastOnce()).isDirectory();
		verify(fEntity, atLeastOnce()).getPath();
		verify(fEntity, atLeastOnce()).getHashPath();
		verify(fEntity, atLeastOnce()).getModified();
		verify(fEntity, atLeastOnce()).getRealm();
		verify(fEntity, atLeastOnce()).getStorage();
	}

	@Test
	void testCreateFromEntity_badRealm() {
		assertThrows(IllegalArgumentException.class,
				() -> FileItemResponse.createFromEntity(fEntity, falseRealm, storage));
		verify(fEntity, atLeastOnce()).getRealm();
	}

	@Test
	void testCreateFromEntity_badStorage() {
		assertThrows(IllegalArgumentException.class,
				() -> FileItemResponse.createFromEntity(fEntity, realm, falseStorage));
		verify(fEntity, atLeastOnce()).getRealm();
		verify(fEntity, atLeastOnce()).getStorage();
	}

}
