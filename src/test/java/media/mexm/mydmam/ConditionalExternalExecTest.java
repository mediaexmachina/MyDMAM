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
package media.mexm.mydmam;

import static java.lang.Runtime.getRuntime;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ExtendWith(ConditionalExternalExecTest.ConditionalExternalExec.class)
public @interface ConditionalExternalExecTest {

	class ConditionalExternalExec implements ExecutionCondition {
		@Override
		public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
			final var method = context.getRequiredTestMethod();
			final var annotation = method.getDeclaredAnnotation(ConditionalExternalExecTest.class);
			if (annotation == null) {
				throw new ExtensionConfigurationException("Could not find @" + ConditionalExternalExecTest.class
														  + " annotation on the method " + method);
			}

			final var cores = getRuntime().availableProcessors();
			return cores > 1 && IS_OS_WINDOWS ? enabled("Windows, multicore")
											  : disabled("No Windows/monocore");
		}
	}

}
