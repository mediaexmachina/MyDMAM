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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryIOProvider.emptyProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import media.mexm.mydmam.entity.FileEntity;

/**
 * Thread safe
 */
public class MetadataThesaurusLogic implements MetadataThesaurusDefaultRegister {
    private static final String TO_STRING = "toString";
    private static final String EQUALS = "equals";
    private static final String HASH_CODE = "hashCode";

    private final ClassLoader classLoader;
    private final Map<Class<?>, Object> instanceByClassName; // TODO check if needed this

    private final Map<Class<?>, MetadataThesaurusInstanceDefinition> definitions;
    private final MetadataThesaurusEntryIOProvider provider;

    // TODO use with provider
    public MetadataThesaurusLogic(final MetadataThesaurusEntryIOProvider provider) {
        classLoader = getClass().getClassLoader();
        instanceByClassName = new ConcurrentHashMap<>();
        definitions = new ConcurrentHashMap<>();
        this.provider = requireNonNull(provider);
    }

    public MetadataThesaurusLogic() {
        this(emptyProvider());
    }

    /**
     * @return key name by methods by className
     */
    public Map<String, Map<String, String>> getImplements() {
        // TODO FIX !!
        final var result = new HashMap<String, Map<String, String>>();

        instanceByClassName.entrySet().forEach(instances -> {
            final var entriesMap = new LinkedHashMap<String, String>();
            definitions.get(instances.getValue())
                    .getEntries()
                    .forEach((m, keyName) -> entriesMap.put(m.getName(), keyName));

            result.put(instances.getKey().getSimpleName(), unmodifiableMap(entriesMap));
        });

        return unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return getImplements().toString();
    }

    public static String nameFormatter(final String name) {
        final var result = new StringBuilder();

        for (var pos = 0; pos < name.length(); pos++) {// NOSONAR 135
            final var character = name.charAt(pos);
            if (pos == 0 && character == '_') {
                continue;
            }

            if (pos == 0
                || pos == 1 && name.charAt(0) == '_') {
                result.append(Character.toLowerCase(character));
                continue;
            }

            if (Character.isUpperCase(character)
                && Character.isLowerCase(name.charAt(pos - 1))
                ||
                pos > 1
                   && Character.isLowerCase(character)
                   && Character.isUpperCase(name.charAt(pos - 1))
                   && Character.isUpperCase(name.charAt(pos - 2))) {
                result.append("-");
            }

            result.append(Character.toLowerCase(character));
        }

        return result.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    @Deprecated
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
                                return null; // definition.getEntryByMethod(method);
                            });
                    // TODO keep this... for getImplements
                    definitions.put(fromClass, new MetadataThesaurusInstanceDefinition(c));
                    return instance;
                });
    }

    @SuppressWarnings("unchecked")
    public <T> T injectInstanceWithIO(final FileEntity fileEntity,
                                      final Class<T> fromClass) {
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

                    final var definition = definitions.computeIfAbsent(fromClass,
                            MetadataThesaurusInstanceDefinition::new);

                    final var classifier = definition.getClassifier();
                    final var key = definition.getKeyNameByMethod(method);

                    return (MetadataThesaurusEntry) newProxyInstance(
                            classLoader,
                            new Class[] { MetadataThesaurusEntry.class },
                            (proxy, subMethod, args) -> onProxyCall(fileEntity, classifier, key, subMethod, args));
                });
    }

    Object onProxyCall(final FileEntity fileEntity,
                       final String classifier,
                       final String key,
                       final Method method,
                       final Object[] args) {
        final var methodName = method.getName();

        if (methodName.equals(HASH_CODE)) {
            throw new UnsupportedOperationException("hashCode is not avaliable from this proxy");
        } else if (methodName.equals(EQUALS)) {
            throw new UnsupportedOperationException("equals is not avaliable from this proxy");
        } else if (methodName.equals(TO_STRING)) {
            return classifier + "." + key;
        } else if (methodName.equals("classifier")) {
            return classifier;
        } else if (methodName.equals("key")) {
            return key;
        } else if (methodName.equals("set")) {
            if (args.length == 2) {
                provider.setValueToDatabase(
                        fileEntity,
                        classifier,
                        key,
                        (int) args[0],
                        String.valueOf(args[1]));// TODO better to string
            } else if (args.length == 1) {
                provider.setValueToDatabase(
                        fileEntity,
                        classifier,
                        key,
                        0,
                        String.valueOf(args[0]));// TODO better to string
            }
        } else if (methodName.equals("get")) {
            if (args.length == 0) {
                return provider.getValueFromDatabase(fileEntity, classifier, key, 0);
            } else if (args.length == 1) {
                return provider.getValueFromDatabase(fileEntity, classifier, key, (int) args[0]);
            }

        } else if (methodName.equals("getAsInt")) {
            if (args.length == 1) {
                return provider.getValueFromDatabase(fileEntity, classifier, key, 0)
                        .map(Integer::valueOf)
                        .orElse((int) args[0]);
            } else if (args.length == 2) {
                return provider.getValueFromDatabase(fileEntity, classifier, key, (int) args[0])
                        .map(Integer::valueOf)
                        .orElse((int) args[1]);
            }
        } else if (methodName.equals("getAll")) {
            return provider.getValueLayerFromDatabase(fileEntity, classifier, key);
        } else if (methodName.equals("getAllInt")) {
            return provider.getValueLayerFromDatabase(fileEntity, classifier, key)
                    .entrySet()
                    .stream()
                    .collect(toUnmodifiableMap(Entry::getKey, e -> Integer.valueOf(e.getValue())));
        }

        throw new IllegalCallerException("Can't manage MetadataThesaurusEntry " + method);
    }

}
