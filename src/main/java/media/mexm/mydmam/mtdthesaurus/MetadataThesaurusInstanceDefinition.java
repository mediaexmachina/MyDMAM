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
import static java.util.function.Predicate.not;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryValueToInjectProvider.emptyProvider;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.ANNOTATION_CLASSIFIER;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.nameFormatter;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Getter;

class MetadataThesaurusInstanceDefinition {

    private final String instanceName;
    private final String parentKey;
    @Getter
    private final Map<Method, MetadataThesaurusEntry> entries;
    @Getter
    private final String classifier;

    private final MetadataThesaurusEntryValueToInjectProvider provider;

    MetadataThesaurusInstanceDefinition(final Class<?> instanceClass) {
        this(instanceClass, emptyProvider());
    }

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

    MetadataThesaurusInstanceDefinition(final Class<?> instanceClass,
                                        final MetadataThesaurusEntryValueToInjectProvider provider) {
        this.provider = provider;
        instanceName = instanceClass.getName();
        final var annotationClassifier = extractClassifier(instanceClass);
        classifier = annotationClassifier.value();
        parentKey = Optional.ofNullable(annotationClassifier.parent()).orElse("");

        final var currentEntries = new LinkedHashMap<Method, MetadataThesaurusEntry>();

        final var methodList = Stream.of(instanceClass.getMethods())
                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                .toList();

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

        methodList.forEach(m -> currentEntries.put(m, methodToEntry(m)));

        entries = Collections.unmodifiableMap(currentEntries);
    }

    private MetadataThesaurusEntry methodToEntry(final Method method) {
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

        return new MetadataThesaurusEntry(classifier, parentKey, nameFormatter(name));
    }

    MetadataThesaurusEntry getEntryByMethod(final Method method) {
        if (entries.containsKey(method) == false) {
            throw new IllegalArgumentException("Can't use " + instanceName + "." + method + ", it's non-accessible.");
        }
        final var entry = entries.get(method);
        return entry.copyWithValue(provider.getValueFromMetadataThesaurusEntry(entry));
    }
}
