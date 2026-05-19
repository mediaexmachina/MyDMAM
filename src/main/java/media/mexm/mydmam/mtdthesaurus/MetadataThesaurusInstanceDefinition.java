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

import static java.lang.Integer.compare;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isInterface;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.NO_UNIT;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.DISPLAYED_AS_IT;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.nameFormatter;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusSortIndexOrder.DEFAULT_VALUE;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Getter;
import media.mexm.mydmam.dto.MtdThesaurusDefEntrySignature;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.MtdRegisterMethodDefinition;

class MetadataThesaurusInstanceDefinition {
    static final String ANNOTATION_CLASSIFIER = MetadataThesaurusClassifier.class.getSimpleName();

    private final String instanceName;
    private final Map<Method, MethodEntryDefinition> entries;
    @Getter
    private final String classifier;
    @Getter
    private final String longName;
    @Getter
    private final MetadataThesaurusClassifierKind kind;

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
        classifier = requireNonNull(annotationClassifier.value());
        longName = requireNonNull(annotationClassifier.longname());
        kind = requireNonNull(annotationClassifier.kind());

        final var methodList = Stream.of(instanceClass.getMethods())
                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                .toList();

        checkInterfaceClass(instanceClass, methodList);

        entries = methodList.stream()
                .collect(toUnmodifiableMap(identity(), this::methodToKeyName));
    }

    static void checkInterfaceClass(final Class<?> instanceClass, final List<Method> methodList) {
        if (isInterface(instanceClass.getModifiers()) == false) {
            throw new IllegalArgumentException("Can't use " + instanceClass + ", it's not an interface");
        }

        final var notAbstract = methodList.stream().filter(not(m -> isAbstract(m.getModifiers()))).toList();
        if (notAbstract.isEmpty() == false) {
            throw new IllegalArgumentException("Can't use " + instanceClass
                                               + ", it containt non-abstract methods: " + notAbstract);
        }
    }

    record MethodEntryDefinition(String instanceClassName,
                                 String classifier,
                                 Method method,
                                 String keyName,
                                 String longname,
                                 MetadataThesaurusEntryType type,
                                 MetadataThesaurusEntryNumericalUnit unit,
                                 int presetOrder) {
    }

    private MethodEntryDefinition methodToKeyName(final Method method) {
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

        final var keyName = nameFormatter(name);
        final var oAttribute = Optional.ofNullable(method.getAnnotation(
                MetadataThesaurusEntryAttribute.class));
        final var longname = oAttribute.map(MetadataThesaurusEntryAttribute::longname)
                .filter(not(String::isBlank))
                .orElseGet(() -> prettifyMethodKeyName(keyName));
        final var type = oAttribute.map(MetadataThesaurusEntryAttribute::type).orElse(DISPLAYED_AS_IT);
        final var unit = oAttribute.map(MetadataThesaurusEntryAttribute::unit).orElse(NO_UNIT);
        final var presetOrder = Optional.ofNullable(method.getAnnotation(
                MetadataThesaurusSortIndexOrder.class))
                .map(MetadataThesaurusSortIndexOrder::value)
                .orElse(DEFAULT_VALUE);

        return new MethodEntryDefinition(
                instanceName,
                classifier,
                method,
                keyName,
                longname,
                type,
                unit,
                presetOrder);
    }

    MethodEntryDefinition getEntryDefinitionByMethod(final Method method) {
        if (entries.containsKey(method) == false) {
            throw new IllegalArgumentException("Can't use " + instanceName + "." + method + ", it's non-accessible.");
        }
        return entries.get(method);
    }

    static String prettifyMethodKeyName(final String keyName) {// TODO test
        if (keyName.length() < 2) {
            return keyName;
        }
        final var longName = keyName.replace("-", " ").trim();
        return longName.substring(0, 1).toUpperCase() + longName.substring(1);
    }

    /**
     * Sorted
     */
    List<MtdRegisterMethodDefinition> extractAllMethodDefinitions(final int classifierSortIndexOrder) { // TODO test
        final var orderedMethods = entries.entrySet().stream()
                .sorted((lEntry, rEntry) -> {
                    final var compare = compare(
                            lEntry.getValue().presetOrder(),
                            rEntry.getValue().presetOrder());
                    if (compare == 0) {
                        return lEntry.getKey().getName()
                                .compareTo(rEntry.getKey().getName());
                    }
                    return compare;
                })
                .map(Entry::getKey)
                .toList();

        return entries.entrySet().stream()
                .map(entry -> {
                    final var method = entry.getKey();
                    final var methodEntryDefinition = entry.getValue();

                    return new MtdRegisterMethodDefinition(
                            method.getName(),
                            methodEntryDefinition.keyName(),
                            new MtdThesaurusDefEntrySignature(
                                    methodEntryDefinition.longname(),
                                    classifierSortIndexOrder * 1000 + orderedMethods.indexOf(method),
                                    methodEntryDefinition.type(),
                                    methodEntryDefinition.unit()));
                })
                .sorted((l, r) -> compare(l.signature().sortIndexOrder(), r.signature().sortIndexOrder()))
                .toList();
    }

}
