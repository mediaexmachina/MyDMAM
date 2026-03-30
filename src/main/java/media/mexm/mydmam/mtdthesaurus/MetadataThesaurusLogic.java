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

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.unmodifiableMap;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.extractClassifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Thread safe
 */
public class MetadataThesaurusLogic implements MetadataThesaurusDefaultRegister {
    private static final String TO_STRING = "toString";

    private static final String EQUALS = "equals";

    private static final String HASH_CODE = "hashCode";

    static final String ANNOTATION_CLASSIFIER = MetadataThesaurusClassifier.class.getSimpleName();

    private final ClassLoader classLoader;
    private final Map<Class<?>, Object> instanceByClassName;
    private final Map<Object, MetadataThesaurusInstanceDefinition> definitions;

    public MetadataThesaurusLogic() {
        classLoader = getClass().getClassLoader();
        instanceByClassName = new ConcurrentHashMap<>();
        definitions = new ConcurrentHashMap<>();
    }

    /**
     * @return MetadataThesaurusEntry by methods by className
     */
    public Map<String, Map<String, MetadataThesaurusEntry>> getImplements() {
        final var result = new HashMap<String, Map<String, MetadataThesaurusEntry>>();

        instanceByClassName.entrySet().forEach(instances -> {
            final var entriesMap = new LinkedHashMap<String, MetadataThesaurusEntry>();
            definitions.get(instances.getValue())
                    .getEntries()
                    .forEach((m, entry) -> entriesMap.put(m.getName(), entry));

            result.put(instances.getKey().getSimpleName(), unmodifiableMap(entriesMap));

        });

        return unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return getImplements().toString();
    }

    /**
     * @see https://www.baeldung.com/java-camel-snake-case-conversion
     */
    public static String nameFormatter(final String name) {
        return name
                .replaceAll("([A-Z])(?=[A-Z])", "$1-")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T makeInstance(final Class<T> fromClass) {
        return (T) instanceByClassName.computeIfAbsent(fromClass,
                c -> {
                    final var instance = newProxyInstance(
                            classLoader,
                            new Class[] { c },
                            (proxy, method, args) -> {
                                if (method.getName().equals(HASH_CODE)) {
                                    return c.hashCode();
                                } else if (method.getName().equals(EQUALS)) {
                                    return args[0].hashCode() == c.hashCode();
                                }
                                final var definition = definitions.get(proxy);
                                if (method.getName().equals(TO_STRING)) {
                                    return definition.getClassifier();
                                }
                                return definition.getEntryByMethod(method);
                            });
                    definitions.put(instance, new MetadataThesaurusInstanceDefinition(c));
                    return instance;
                });
    }

    @SuppressWarnings("unchecked")
    public <T> T injectInstanceReadEntities(final Function<MetadataThesaurusEntry, Optional<String>> valueProvider,
                                            final Class<T> fromClass) {
        final var definition = new MetadataThesaurusInstanceDefinition(fromClass, valueProvider::apply);

        return (T) newProxyInstance(
                classLoader,
                new Class[] { fromClass },
                (_, method, _) -> {
                    if (method.getName().equals(HASH_CODE)) {
                        throw new UnsupportedOperationException("hashCode is not avaliable from this proxy");
                    } else if (method.getName().equals(EQUALS)) {
                        throw new UnsupportedOperationException("equals is not avaliable from this proxy");
                    } else if (method.getName().equals(TO_STRING)) {
                        throw new UnsupportedOperationException("toString is not avaliable from this proxy");
                    }
                    return definition.getEntryByMethod(method);
                });
    }

    @SuppressWarnings("unchecked")
    public <T> T injectInstanceWriteEntities(final Consumer<MetadataThesaurusEntry> updateEntry,
                                             final Class<T> fromClass) {
        final var annotationClassifier = extractClassifier(fromClass);

        return (T) newProxyInstance(
                classLoader,
                new Class[] { fromClass },
                (_, method, _) -> {
                    if (method.getName().equals(HASH_CODE)) {
                        throw new UnsupportedOperationException("hashCode is not avaliable from this proxy");
                    } else if (method.getName().equals(EQUALS)) {
                        throw new UnsupportedOperationException("equals is not avaliable from this proxy");
                    } else if (method.getName().equals(TO_STRING)) {
                        throw new UnsupportedOperationException("toString is not avaliable from this proxy");
                    }

                    updateEntry.accept(
                            new MetadataThesaurusEntry(
                                    annotationClassifier.value(),
                                    annotationClassifier.parent(),
                                    nameFormatter(method.getName())));

                    return null;
                });
    }

}
