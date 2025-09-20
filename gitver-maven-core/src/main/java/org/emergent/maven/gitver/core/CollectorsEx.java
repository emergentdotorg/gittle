package org.emergent.maven.gitver.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class CollectorsEx {

    private static final Collector.Characteristics CH_ID = Collector.Characteristics.IDENTITY_FINISH;

    private static final boolean USE_SORTED_MAP = false;

    private static <K, V> Supplier<Map<K, V>> getDefaultMapSupplier() {
        return USE_SORTED_MAP ? TreeMap::new : LinkedHashMap::new;
    }

    private static <K, V> UnaryOperator<Map<K, V>> getDefaultMapConstructor() {
        return USE_SORTED_MAP ? TreeMap::new : LinkedHashMap::new;
    }

    public static <K,V> Map<K,V> wrap(Map<K,V> map) {
        UnaryOperator<Map<K, V>> op = getDefaultMapConstructor();
        return op.apply(map);
    }

    public static <T extends Map.Entry<K, V>, K, V, R> Collector<T, Map<K, V>, R> toMapAndThen(
            Function<Map<K, V>, R> finisher
    ) {
        return toMap(T::getKey, T::getValue, getDefaultMapSupplier(), finisher);
    }

    public static <T extends Map.Entry<K, V>, K, V> Collector<T, ?, Map<K, V>> toMap() {
        return toMap(T::getKey, T::getValue, getDefaultMapSupplier());
    }

    public static <T extends Map.Entry<K, V>, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap() {
        return CollectorsEx.toMap(LinkedHashMap::new);
    }

    public static <T extends Map.Entry<K, V>, K, V> Collector<T, ?, Map<K, V>> toTreeMap() {
        return CollectorsEx.toMap(TreeMap::new);
    }

    public static <T extends Map.Entry<K, V>, R extends Map<K, V>, K, V> Collector<T, ?, R> toMap(
            Supplier<R> mapSupplier
    ) {
        return toMap(T::getKey, T::getValue, mapSupplier);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valMapper) {
        return toMap(keyMapper, valMapper, getDefaultMapSupplier());
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedHashMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valMapper) {
        return toMap(keyMapper, valMapper, LinkedHashMap::new);
    }

    public static <T, K, U> Collector<T, ?, Map<K, U>> toTreeMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valMapper) {
        return toMap(keyMapper, valMapper, TreeMap::new);
    }

    public static <T, R extends Map<K, V>, K, V> Collector<T, R, R> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper,
            Supplier<R> mapSupplier) {
        return toMap(keyMapper, valMapper, mapSupplier, Function.identity());
    }

    public static <T, A extends Map<K, V>, K, V, R> Collector<T, A, R> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valMapper,
            Supplier<A> mapSupplier,
            Function<A, R> finisher) {
        return Collector.of(
                mapSupplier,
                uniqKeysMapAccumulator(keyMapper, valMapper),
                uniqKeysMapMerger(),
                finisher,
                CH_ID);
    }

    private static <T, A, R, RR> Collector<T, A, RR> collectingAndThen(
            Collector<T, A, R> downstream, Function<R, RR> finisher
    ) {
        return Collectors.collectingAndThen(downstream, finisher);
    }

    public static <T, K, V, R extends Map<K, V>> BiConsumer<R, T> uniqKeysMapAccumulator(
            Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return (map, element) -> {
            K k = keyMapper.apply(element);
            V v = Objects.requireNonNull(valueMapper.apply(element));
            V u = map.putIfAbsent(k, v);
            if (u != null) {
                throw duplicateKeyException(k, u, v);
            }
        };
    }

    public static <K, V, M extends Map<K, V>> BinaryOperator<M> uniqKeysMapMerger() {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                K k = e.getKey();
                V v = Objects.requireNonNull(e.getValue());
                V u = m1.putIfAbsent(k, v);
                if (u != null) {
                    throw duplicateKeyException(k, u, v);
                }
            }
            return m1;
        };
    }

    private static <U> BinaryOperator<U> getMergeFunction() {
        return (a, b) -> {
            throw new IllegalStateException("Duplicate key! a=" + a + "; b=" + b);
        };
    }

    public static IllegalStateException duplicateKeyException(Object k, Object u, Object v) {
        return new IllegalStateException(
                String.format("Duplicate key %s (attempted merging values %s and %s)", k, u, v));
    }
}
