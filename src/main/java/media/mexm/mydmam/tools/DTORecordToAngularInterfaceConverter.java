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
package media.mexm.mydmam.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DTORecordToAngularInterfaceConverter {

	private static final String ENUM = "enum";
	private static final String INTERFACE = "interface";
	private static final String STRING = "string";
	private static final String BOOLEAN = "boolean";
	private static final String NUMBER = "number";
	private final Map<Class<?>, String> defaultClassConverter;
	private final File angularDtoDirectory;
	private final Set<Class<?>> dtoClasses;

	public DTORecordToAngularInterfaceConverter(final String dtoPackageName, final File angularProjectDestination) {
		defaultClassConverter = new HashMap<>();
		defaultClassConverter.put(String.class, STRING);
		defaultClassConverter.put(int.class, NUMBER);
		defaultClassConverter.put(Integer.class, NUMBER);
		defaultClassConverter.put(short.class, NUMBER);
		defaultClassConverter.put(Short.class, NUMBER);
		defaultClassConverter.put(float.class, NUMBER);
		defaultClassConverter.put(Float.class, NUMBER);
		defaultClassConverter.put(long.class, NUMBER);
		defaultClassConverter.put(Long.class, NUMBER);
		defaultClassConverter.put(double.class, NUMBER);
		defaultClassConverter.put(Double.class, NUMBER);
		defaultClassConverter.put(boolean.class, BOOLEAN);
		defaultClassConverter.put(Boolean.class, BOOLEAN);

		if (angularProjectDestination.exists() == false
			|| angularProjectDestination.isDirectory() == false
			|| angularProjectDestination.canWrite() == false) {
			throw new UncheckedIOException(new FileNotFoundException(
					"Can't found " + angularProjectDestination + " as directory."));
		}

		angularDtoDirectory = new File(angularProjectDestination, "dto");
		try {
			forceMkdir(angularDtoDirectory);
			cleanDirectory(angularDtoDirectory);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}

		/**
		 * https://www.baeldung.com/java-find-all-classes-in-package
		 */
		final var stream = ClassLoader.getSystemClassLoader()
				.getResourceAsStream(dtoPackageName.replaceAll("[.]", "/"));

		try (final var reader = new BufferedReader(new InputStreamReader(stream))) {
			dtoClasses = reader.lines()
					.filter(line -> line.endsWith(".class"))
					.map(className -> {
						try {
							return Class.forName(dtoPackageName + "."
												 + className.substring(0, className.lastIndexOf('.')));
						} catch (final ClassNotFoundException e) {// NOSONAR S108
						}
						return null;
					})
					.filter(not(Objects::isNull))
					.collect(toUnmodifiableSet());
		} catch (final IOException e1) {
			throw new UncheckedIOException(e1);
		}

		if (dtoClasses.isEmpty()) {
			throw new IllegalArgumentException("Can't found DTO classes in " + dtoPackageName);
		}

	}

	static List<Class<?>> getGenericTypeArg(final Field field) {
		final List<Class<?>> result = new ArrayList<>();
		if (field.getGenericType() instanceof final ParameterizedType genericType) { // NOSONAR S1191
			for (final var genericTypeArg : genericType.getActualTypeArguments()) {
				result.add((Class<?>) genericTypeArg);
			}
		} else {
			log.warn("Can't extract generic type from {}", field);
		}
		return result;
	}

	void classCrawler(final Set<Class<?>> foundedClass, final Class<?> cls) {
		if (foundedClass.contains(cls) || defaultClassConverter.containsKey(cls)) {
			return;
		}
		foundedClass.add(cls);

		Stream.of(cls.getDeclaredFields())
				.filter(field -> defaultClassConverter.containsKey(field.getType()) == false)
				.forEach(field -> {
					var type = field.getType();
					if (type.isArray()) {
						type = type.getComponentType();
					}

					if (List.class.isAssignableFrom(type)
						|| Set.class.isAssignableFrom(type)
						|| Map.class.isAssignableFrom(type)) {
						getGenericTypeArg(field).forEach(genericTypeArg -> classCrawler(foundedClass, genericTypeArg));
					} else {
						classCrawler(foundedClass, type);
					}
				});
	}

	public void parseDTOs() {
		/**
		 * Step one, full class scan.
		 */
		final Set<Class<?>> foundedClass = new HashSet<>();
		log.info("Prepare for DTO exports {}", dtoClasses);
		dtoClasses.forEach(cls -> classCrawler(foundedClass, cls));

		/**
		 * Step two, create angular files for used classes.
		 */
		foundedClass.forEach(cls -> {
			try {
				log.info("Export as DTO {}", cls);
				makeAngularFile(cls, processClass(cls));
			} catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	List<ClassFieldToAngular> processClass(final Class<?> cls) {// NOSONAR S3776
		return Stream.of(cls.getDeclaredFields())
				.map(field -> {
					final var name = field.getName();
					final var type = field.getType();

					if (defaultClassConverter.containsKey(type)) {
						return new ClassFieldToAngular(
								name,
								defaultClassConverter.get(type),
								Map.of(),
								false);
					}

					final var importTypes = new HashMap<String, String>();

					if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
						final var genericTypeArgs = getGenericTypeArg(field);

						if (genericTypeArgs.isEmpty() == false) {
							final var genericTypeArg = genericTypeArgs.get(0);
							final var angularType = defaultClassConverter.getOrDefault(
									genericTypeArg, genericTypeArg.getSimpleName());

							if (defaultClassConverter.containsKey(genericTypeArg) == false) {
								importTypes.put(angularType, genericTypeArg.isEnum() ? ENUM : INTERFACE);
							}

							return new ClassFieldToAngular(
									name,
									"Array<" + angularType + ">",
									importTypes,
									false);
						} else {
							return new ClassFieldToAngular(
									name,
									"Array",
									Map.of(),
									false);
						}
					} else if (Map.class.isAssignableFrom(type)) {
						final var genericTypeArgs = getGenericTypeArg(field);

						if (genericTypeArgs.isEmpty() == false) {
							final var genericTypeArgK = genericTypeArgs.get(0);
							final var genericTypeArgV = genericTypeArgs.get(1);

							final var angularTypeK = defaultClassConverter.getOrDefault(
									genericTypeArgK, genericTypeArgK.getSimpleName());
							final var angularTypeV = defaultClassConverter.getOrDefault(
									genericTypeArgV, genericTypeArgV.getSimpleName());

							if (defaultClassConverter.containsKey(genericTypeArgK) == false) {
								importTypes.put(angularTypeK, genericTypeArgK.isEnum() ? ENUM : INTERFACE);
							}
							if (defaultClassConverter.containsKey(genericTypeArgV) == false) {
								importTypes.put(angularTypeV, genericTypeArgV.isEnum() ? ENUM : INTERFACE);
							}

							return new ClassFieldToAngular(
									name,
									"Map<" + angularTypeK + ", " + angularTypeV + ">",
									importTypes,
									false);
						} else {
							return new ClassFieldToAngular(
									name,
									"Map",
									Map.of(),
									false);
						}
					} else if (cls.isEnum() && type.isArray()) {
						return null;
					} else if (cls.isEnum()) {
						return new ClassFieldToAngular(
								name,
								null,
								Map.of(),
								true);
					} else {
						return new ClassFieldToAngular(
								name,
								type.getSimpleName(),
								Map.of(type.getSimpleName(), type.isEnum() ? ENUM : INTERFACE),
								false);
					}
				})
				.filter(Objects::nonNull)
				.toList();
	}

	void makeAngularFile(final Class<?> cls, final List<ClassFieldToAngular> fieldList) throws IOException {
		final var fileType = cls.isEnum() ? ENUM : INTERFACE;
		final var fileBaseBame = getAngularFileName(cls.getSimpleName(), fileType);

		final var fileContent = new ArrayList<String>();
		fileContent.add("/*");
		fileContent.add("  AUTOGENERATED FILE! DO NOT EDIT!");
		fileContent.add("  GENERATED BY '" + getClass().getSimpleName() + ".java'");
		fileContent.add("  FROM THE JAVA SIDE OF THIS PROJECT");
		fileContent.add("*/");

		fileContent.addAll(fieldList.stream()
				.filter(f -> f.importTypes().isEmpty() == false)
				.flatMap(ClassFieldToAngular::makeImportLines)
				.distinct()
				.sorted()
				.toList());

		if (fieldList.stream()
				.map(ClassFieldToAngular::importTypes)
				.anyMatch(not(Map::isEmpty))) {
			fileContent.add("");
		}

		fileContent.add("export " + fileType + " " + cls.getSimpleName() + " {");

		fileContent.addAll(fieldList.stream()
				.map(ClassFieldToAngular::makeDefinitionLine)
				.toList());

		fileContent.add("}");

		final var interfaceFile = new File(angularDtoDirectory, fileBaseBame + ".ts");
		write(interfaceFile, fileContent.stream().collect(joining("\n")), UTF_8);
	}

	/**
	 * https://www.baeldung.com/java-camel-snake-case-conversion
	 */
	static String getAngularFileName(final String className, final String suffix) {
		return className
				.replaceAll("([A-Z])(?=[A-Z])", "$1-")
				.replaceAll("([a-z])([A-Z])", "$1-$2")
				.toLowerCase() + "." + suffix;
	}

}
