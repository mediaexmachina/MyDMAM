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
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.checkInterfaceClass;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread safe
 */
@Slf4j
public class MetadataThesaurusLogic {
    static final String TO_STRING = "toString";
    static final String EQUALS = "equals";
    static final String HASH_CODE = "hashCode";

    private final ClassLoader classLoader;

    private final Map<Class<?>, MetadataThesaurusInstanceDefinition> definitions;
    private final Map<Method, Class<?>> registerDefinitions;

    public MetadataThesaurusLogic() {
        classLoader = getClass().getClassLoader();
        definitions = new ConcurrentHashMap<>();

        final var instanceClass = MetadataThesaurusRegister.class;
        final var methodList = Stream.of(instanceClass.getMethods()).toList();
        checkInterfaceClass(instanceClass, methodList);
        registerDefinitions = methodList.stream().collect(toUnmodifiableMap(m -> m, Method::getReturnType));
    }

    Map<Method, Class<?>> getRegisterDefinitions() {
        return registerDefinitions;
    }

    public record MtdRegisterMethodDefinition(String methodName, String keyName) {
    }

    public record MtdRegisterDefinition(String className, String classifier,
                                        List<MtdRegisterMethodDefinition> methods) {
    }

    /**
     * Sorted
     */
    public List<MtdRegisterDefinition> getImplementsFromRegister() {
        return registerDefinitions.values()
                .stream()
                .map(thesaurusDefinitionClass -> {
                    final var className = thesaurusDefinitionClass.getSimpleName();
                    final var instanceDef = new MetadataThesaurusInstanceDefinition(thesaurusDefinitionClass);
                    final var classifier = instanceDef.getClassifier();

                    final var methodsDefs = instanceDef.getAllMethods()
                            .stream()
                            .map(method -> {
                                final var methodName = method.getName();
                                final var keyName = instanceDef.getKeyNameByMethod(method);
                                return new MtdRegisterMethodDefinition(methodName, keyName);
                            })
                            .sorted((l, r) -> l.methodName.compareTo(r.methodName))
                            .toList();

                    return new MtdRegisterDefinition(className, classifier, methodsDefs);
                })
                .sorted((l, r) -> l.className.compareTo(r.className))
                .toList();
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

    public MetadataThesaurusRegister makeRegister(final MetadataThesaurusEntryIOProvider provider) {
        requireNonNull(provider, "\"provider\" can't to be null");

        return (MetadataThesaurusRegister) newProxyInstance(
                classLoader,
                new Class[] { MetadataThesaurusRegister.class },
                (_, method, args) -> {
                    checkMethodNotHaveArgs(method, args);
                    checkMethodNotHashCode(method);
                    checkMethodNotEquals(method);
                    checkMethodNotToString(method);
                    return injectInstanceWithIO(provider, registerDefinitions.get(method));
                });
    }

    @SuppressWarnings("unchecked")
    <T> T injectInstanceWithIO(final MetadataThesaurusEntryIOProvider provider,
                               final Class<T> fromClass) {
        requireNonNull(provider, "\"provider\" can't to be null");
        requireNonNull(fromClass, "\"fromClass\" can't to be null");

        return (T) newProxyInstance(
                classLoader,
                new Class[] { fromClass },
                (_, method, mArgs) -> {
                    checkMethodNotHaveArgs(method, mArgs);
                    checkMethodNotHashCode(method);
                    checkMethodNotEquals(method);
                    checkMethodNotToString(method);

                    final var definition = definitions.computeIfAbsent(fromClass,
                            MetadataThesaurusInstanceDefinition::new);
                    final var classifier = definition.getClassifier();
                    final var key = definition.getKeyNameByMethod(method);

                    return (MetadataThesaurusEntry) newProxyInstance(
                            classLoader,
                            new Class[] { MetadataThesaurusEntry.class },
                            (_, subMethod, args) -> onEntryCall(provider, classifier, key, subMethod.getName(), args));
                });
    }

    /**
     * @see MetadataThesaurusEntry
     */
    @SuppressWarnings("unchecked")
    Object onEntryCall(final MetadataThesaurusEntryIOProvider provider,
                       final String classifier,
                       final String key,
                       final String methodName,
                       final Object[] args) {
        return switch (methodName) {
        case HASH_CODE -> throw new UnsupportedOperationException("hashCode is not avaliable from this proxy");
        case EQUALS -> throw new UnsupportedOperationException("equals is not avaliable from this proxy");
        case TO_STRING -> classifier + "." + key;
        case "classifier" -> classifier;
        case "key" -> key;
        case "set" -> {
            if (args.length == 2) {
                set(args[1]).ifPresent(v -> provider.setValueToDatabase(
                        classifier,
                        key,
                        (int) args[0],
                        v));
            } else if (args.length == 1) {
                set(args[0]).ifPresent(v -> provider.setValueToDatabase(
                        classifier,
                        key,
                        0,
                        v));
            }
            yield null;
        }
        case "setDateISO8601" -> {
            if (args.length == 2) {
                setDateISO8601((Optional<String>) args[1])
                        .ifPresent(v -> provider.setValueToDatabase(
                                classifier,
                                key,
                                (int) args[0],
                                v));
            } else if (args.length == 1) {
                setDateISO8601((Optional<String>) args[0])
                        .ifPresent(v -> provider.setValueToDatabase(
                                classifier,
                                key,
                                0,
                                v));
            }
            yield null;
        }
        case "get" -> {
            if (args == null) {
                yield provider.getValueFromDatabase(classifier, key, 0);
            } else if (args.length == 1) {
                yield provider.getValueFromDatabase(classifier, key, (int) args[0]);
            }
            throw new IllegalCallerException("Can't manage MetadataThesaurusEntry " + methodName);
        }
        case "getAsInt" -> {
            if (args.length == 1) {
                yield provider.getValueFromDatabase(classifier, key, 0)
                        .map(Integer::valueOf)
                        .orElse((int) args[0]);
            } else if (args.length == 2) {
                yield provider.getValueFromDatabase(classifier, key, (int) args[0])
                        .map(Integer::valueOf)
                        .orElse((int) args[1]);
            }
            throw new IllegalCallerException("Can't manage MetadataThesaurusEntry " + methodName);
        }
        case "getAll" -> provider.getValueLayerFromDatabase(classifier, key);
        case "getAllInt" -> provider.getValueLayerFromDatabase(classifier, key)
                .entrySet()
                .stream()
                .collect(toUnmodifiableMap(Entry::getKey, e -> Integer.valueOf(e.getValue())));
        default -> throw new IllegalCallerException("Can't manage MetadataThesaurusEntry " + methodName);
        };
    }

    static Optional<String> setDateISO8601(final Optional<String> oValue) {
        requireNonNull(oValue);
        if (oValue.isEmpty()) {
            return empty();
        }

        try {
            return oValue
                    .map(String::trim)
                    .filter(not(String::isEmpty))
                    .map(Instant::parse)
                    .map(Instant::toEpochMilli)
                    .map(String::valueOf);
        } catch (final DateTimeParseException e) {
            log.warn("Can't parse date: \"{}\"", oValue.orElse(""));
            return empty();
        }
    }

    static Optional<String> set(final Object value) {
        if (value == null) {
            return empty();
        } else if (value instanceof final String s) {
            if (s.isBlank()) {
                return empty();
            }
            return Optional.ofNullable(s);
        } else if (value instanceof final Duration d) {
            return set(String.valueOf(d.toMillis()));
        } else if (value instanceof final Optional<?> o) {
            if (o.isPresent()) {
                return set(o.get());
            } else {
                return empty();
            }
        } else {
            return set(String.valueOf(value));
        }
    }

    static void checkMethodNotHaveArgs(final Method method, final Object[] args) {
        if (args != null && args.length > 0) {
            throw new UnsupportedOperationException(method.getName() + " is not avaliable from this proxy");
        }
    }

    static void checkMethodNotHashCode(final Method method) {
        if (method.getName().equals(HASH_CODE)) {
            throw new UnsupportedOperationException("hashCode is not avaliable from this proxy");
        }
    }

    static void checkMethodNotToString(final Method method) {
        if (method.getName().equals(TO_STRING)) {
            throw new UnsupportedOperationException("toString is not avaliable from this proxy");
        }
    }

    static void checkMethodNotEquals(final Method method) {
        if (method.getName().equals(EQUALS)) {
            throw new UnsupportedOperationException("equals is not avaliable from this proxy");
        }
    }

}
