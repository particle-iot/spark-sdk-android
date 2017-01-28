package io.particle.android.sdk.utils;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;


/**
 * Some functional-style utilities for processing collections
 */
@ParametersAreNonnullByDefault
public class Funcy {


    public interface Predicate<T> {
        boolean test(T testTarget);
    }


    public interface Func<In, Out> {
        Out apply(In in);
    }

    // these are available as methods to enable generics handling
    public static <T> Predicate<T> alwaysTrue() {
        //noinspection unchecked
        return (Predicate<T>) alwaysTrue;
    }

    public static <T> Predicate<T> alwaysFalse() {
        //noinspection unchecked
        return (Predicate<T>) alwaysFalse;
    }

    public static <T> Predicate<T> notNull() {
        //noinspection unchecked
        return (Predicate<T>) notNull;
    }

    public static <In, Out> List<Out> transformList(@Nullable List<In> sourceList,
                                                    Func<In, Out> transformFunc) {
        return transformList(sourceList, null, transformFunc, null);
    }

    public static <In, Out> List<Out> transformList(@Nullable List<In> sourceList,
                                                    Predicate<In> inTypeInclusionFilter,
                                                    Func<In, Out> transformFunc) {
        return transformList(sourceList, inTypeInclusionFilter, transformFunc, null);
    }

    public static <In, Out> List<Out> transformList(@Nullable List<In> sourceList,
                                                    Func<In, Out> transformFunc,
                                                    Predicate<Out> outTypeInclusionFilter) {
        return transformList(sourceList, null, transformFunc, outTypeInclusionFilter);
    }

    public static <In, Out> List<Out> transformList(@Nullable List<In> sourceList,
                                                    @Nullable Predicate<In> inTypeInclusionFilter,
                                                    Func<In, Out> transformFunc,
                                                    @Nullable Predicate<Out> outTypeInclusionFilter) {
        return (List<Out>) transformCollection(sourceList, inTypeInclusionFilter, transformFunc,
                outTypeInclusionFilter, listFactory);
    }

    public static <In, Out> Set<Out> transformSet(@Nullable Set<In> sourceSet,
                                                  Func<In, Out> transformFunc) {
        return transformSet(sourceSet, null, transformFunc, null);
    }

    public static <In, Out> Set<Out> transformSet(@Nullable Set<In> sourceSet,
                                                  Predicate<In> inTypeInclusionFilter,
                                                  Func<In, Out> transformFunc) {
        return transformSet(sourceSet, inTypeInclusionFilter, transformFunc, null);
    }

    public static <In, Out> Set<Out> transformSet(@Nullable Set<In> sourceSet,
                                                  Func<In, Out> transformFunc,
                                                  Predicate<Out> outTypeInclusionFilter) {
        return transformSet(sourceSet, null, transformFunc, outTypeInclusionFilter);
    }

    public static <In, Out> Set<Out> transformSet(@Nullable Set<In> sourceSet,
                                                  @Nullable Predicate<In> inTypeInclusionFilter,
                                                  Func<In, Out> transformFunc,
                                                  @Nullable Predicate<Out> outTypeInclusionFilter) {
        return (Set<Out>) transformCollection(sourceSet, inTypeInclusionFilter, transformFunc,
                outTypeInclusionFilter, setFactory);
    }

    public static <T> List<T> filter(@Nullable List<T> toFilter, Predicate<T> predicate) {
        return (List<T>) filterCollection(toFilter, predicate, listFactory);
    }

    public static <T> Set<T> filter(@Nullable Set<T> toFilter, Predicate<T> predicate) {
        return (Set<T>) filterCollection(toFilter, predicate, setFactory);
    }

    @Nullable
    public static <T> T findFirstMatch(Collection<T> items, Predicate<T> predicate) {
        for (T item : items) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }


    private static <In, Out, C extends Collection> Collection<Out> transformCollection(
            @Nullable Collection<In> source, @Nullable Predicate<In> inTypeInclusionFilter,
            Func<In, Out> transformFunc, @Nullable Predicate<Out> outTypeInclusionFilter,
            CollectionFactory<C> collectionFactory) {
        if (source == null || source.isEmpty()) {
            //noinspection unchecked
            return collectionFactory.emptyCollection();
        }

        //noinspection unchecked
        Collection<Out> result = collectionFactory.newWithCapacity(source.size());
        for (In fromItem : source) {
            if (inTypeInclusionFilter != null && !inTypeInclusionFilter.test(fromItem)) {
                continue;
            }

            Out transformed = transformFunc.apply(fromItem);
            if (outTypeInclusionFilter == null || outTypeInclusionFilter.test(transformed)) {
                result.add(transformed);
            }
        }
        return result;
    }


    private static <T, C extends Collection> Collection<T> filterCollection(
            @Nullable Collection<T> toFilter, Predicate<T> predicate, CollectionFactory<C> collectionFactory) {
        if (toFilter == null || toFilter.isEmpty()) {
            //noinspection unchecked
            return collectionFactory.emptyCollection();
        }

        //noinspection unchecked
        Collection<T> result = collectionFactory.newWithCapacity(toFilter.size());
        for (T item : toFilter) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }

        return result;
    }


    private static final Predicate<?> alwaysTrue = (Predicate<Object>) testTarget -> true;
    private static final Predicate<?> alwaysFalse = (Predicate<Object>) testTarget -> false;
    private static final Predicate<?> notNull = (Predicate<Object>) testTarget -> testTarget != null;


    private interface CollectionFactory<C extends Collection> {
        C newWithCapacity(int size);

        C emptyCollection();
    }


    private static final CollectionFactory<List> listFactory = new CollectionFactory<List>() {
        @Override
        public List<?> newWithCapacity(int size) {
            return new ArrayList(size);
        }

        @Override
        public List<?> emptyCollection() {
            return Collections.emptyList();
        }
    };


    private static final CollectionFactory<Set> setFactory = new CollectionFactory<Set>() {
        @Override
        public Set<?> newWithCapacity(int size) {
            return new HashSet(size);
        }

        @Override
        public Set<?> emptyCollection() {
            return Collections.emptySet();
        }
    };

}