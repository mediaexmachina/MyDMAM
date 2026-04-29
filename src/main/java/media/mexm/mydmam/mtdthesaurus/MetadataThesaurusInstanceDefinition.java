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
package media.mexm.mydmam.mtdthesaurus;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.nameFormatter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import lombok.Getter;

class MetadataThesaurusInstanceDefinition {
    static final String ANNOTATION_CLASSIFIER = MetadataThesaurusClassifier.class.getSimpleName();

    private final String instanceName;
    private final Map<Method, String> entries;
    @Getter
    private final String classifier;

    static MetadataThesaurusClassifier extractClassifier(final Class<?> instanceClass) {
        final var annotationClassifier = Optional.ofNullable(instanceClass.getAnnotation(
                MetadataThesaurusClassifier.class))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Can't use " + instanceClass + ", missing " + ANNOTATION_CLASSIFIER + " annotation"));

        final var classifier = annotationClassifier.value();
        if (classifier.isEmpty()) {
            throw new IllegalArgumentException("Can't use " + instanceClass
                                               + ", you must set a classifier name in annotation");
        }
        if (classifier.contains(".")) {
            throw new IllegalArgumentException("Can't use " + instanceClass
                                               + ", invalid classifier name: " + classifier);
        }
        return annotationClassifier;
    }

    MetadataThesaurusInstanceDefinition(final Class<?> instanceClass) {
        instanceName = instanceClass.getName();
        final var annotationClassifier = extractClassifier(instanceClass);
        classifier = annotationClassifier.value();

        final var methodList = Stream.of(instanceClass.getMethods())
                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                .toList();

        checkInterfaceClass(instanceClass, methodList);

        entries = methodList.stream()
                .collect(toUnmodifiableMap(identity(), this::methodToKeyName));
    }

    static void checkInterfaceClass(final Class<?> instanceClass, final List<Method> methodList) {
        final var notAbstract = methodList.stream().filter(not(m -> isAbstract(m.getModifiers()))).toList();
        if (notAbstract.isEmpty() == false) {
            throw new IllegalArgumentException("Can't use " + instanceClass
                                               + ", it containt non-abstract methods: " + notAbstract);
        }

        final var notPublic = methodList.stream().filter(not(m -> isPublic(m.getModifiers()))).toList();
        if (notPublic.isEmpty() == false) {
            throw new IllegalArgumentException("Can't use " + instanceClass
                                               + ", it containt non-public methods: " + notPublic);
        }
    }

    private String methodToKeyName(final Method method) {
        final var name = method.getName();
        if (method.getParameterCount() > 0) {
            throw new IllegalArgumentException(
                    "Can't manage proxy methods with args "
                                               + " on (" + instanceName + "." + name
                                               + ")");
        } else if (MetadataThesaurusEntry.class.isAssignableFrom(method
                .getReturnType()) == false) {
            throw new IllegalArgumentException(
                    "Can't manage methods return type different than "
                                               + MetadataThesaurusEntry.class
                                                       .getSimpleName()
                                               + " (on " + instanceName + "." + name
                                               + ")");
        }
        return nameFormatter(name);
    }

    Set<Method> getAllMethods() {
        return entries.keySet();
    }

    String getKeyNameByMethod(final Method method) {
        if (entries.containsKey(method) == false) {
            throw new IllegalArgumentException("Can't use " + instanceName + "." + method + ", it's non-accessible.");
        }
        return entries.get(method);
    }
}
