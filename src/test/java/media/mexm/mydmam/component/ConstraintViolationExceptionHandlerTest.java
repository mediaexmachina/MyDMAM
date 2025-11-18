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
package media.mexm.mydmam.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ConstraintViolationExceptionHandlerTest {

	@Mock
	ConstraintViolationException e;
	@Mock
	WebRequest request;

	ConstraintViolationExceptionHandler c;

	@BeforeEach
	void init() {
		c = new ConstraintViolationExceptionHandler();
	}

	@Test
	void testHandleConstraintViolation() {
		final var result = c.handleConstraintViolation(e, request);
		assertThat(result.getStatusCode()).isEqualTo(BAD_REQUEST);
		verify(e, atLeastOnce()).getMessage();
	}

}
