package io.particle.android.sdk.utils;

import android.support.v4.util.ArrayMap;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Python-inspired painkillers for Java
 * <p>
 * For extra Pythonic happiness, use these as static imports, and you can
 * do things like <code>list()</code>, the way you could in Python!
 *
 * @author jens.knutson@gmail.com
 */
public class Py {

    /**
     * Test the "truthiness" of an object, ala Python - see http://goo.gl/JebVU
     * <p>
     * The following will all return <code>false</code>:
     * <ul>
     * <li>null
     * <li>Any empty collection/array/iterable
     * <li>a empty string, i.e.: ""
     * <li>a Number (like Integer or Long, or their primitive equivalents) with a value of 0
     * <li>A Boolean (or primitive equiv.) which evaluates to false
     * </ul>
     * <p>
     * Anything else will return true.
     *
     * @param obj the target to be evaluated
     * @return is obj "truthy"
     */
    public static boolean truthy(Object obj) {
        if (obj == null) {
            return false;

        } else if (obj instanceof Collection) {
            return (!((Collection<?>) obj).isEmpty());

        } else if (obj instanceof Iterable) {
            return !((Iterable<?>) obj).iterator().hasNext();

        } else if (obj instanceof Object[]) {
            return (((Object[]) obj).length > 0);

        } else if (obj instanceof Number) {
            return (((Number) obj).longValue() != 0);

        } else if (obj instanceof CharSequence) {
            return (((CharSequence) obj).length() > 0);

        } else if (obj instanceof JSONArray) {
            return (((JSONArray) obj).length() > 0);

        } else if (obj instanceof Boolean) {
            return ((Boolean) obj);

        } else if (obj instanceof long[]) {
            return (((long[]) obj).length > 0);

        } else if (obj instanceof int[]) {
            return (((int[]) obj).length > 0);

        } else if (obj instanceof short[]) {
            return (((short[]) obj).length > 0);

        } else if (obj instanceof byte[]) {
            return (((byte[]) obj).length > 0);

        } else if (obj instanceof char[]) {
            return (((char[]) obj).length > 0);

        } else if (obj instanceof boolean[]) {
            return (((boolean[]) obj).length > 0);

        } else if (obj instanceof float[]) {
            return (((float[]) obj).length > 0);

        } else if (obj instanceof double[]) {
            return (((double[]) obj).length > 0);
        }

        return true;
    }

    // NOTE: "truthy()" is by far the most popular function in this file, and
    // it serves as the basis for many of the other functions below. If you
    // don't know how it works, take a second to review it - it's easy and
    // really useful in taking away some of the lameness and pain of
    // overly verbose code.

    /**
     * Return true *only* if *every* object in the varargs array is truthy
     * <p>
     * Call truthy() on each object in the varargs - this is called all() in
     * Python, so that's what I'm calling it here.
     */
    public static boolean all(Object... objects) {
        if (!truthy(objects)) {
            // is our varargs list null or empty?
            return false;
        }

        for (Object obj : objects) {
            if (!truthy(obj)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if *any* object in the varargs array is truthy
     * <p>
     * Calls truthy() on each object in the varargs - this is called any() in
     * Python, so that's what I'm calling it here.
     */
    public static boolean any(Object... objects) {
        if (!truthy(objects)) {
            // is our varargs list null or empty?
            return false;
        }

        for (Object obj : objects) {
            if (truthy(obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A simple function to create lists without all the extra noise.
     * <p>
     * Quick quiz: unless you have special, specific requirements, like
     * thread safety or immutability, which List implementation do you use
     * <em>every time</em>?  It's ArrayList, isn't it?
     * The Py class embraces this fact and gives you Py.list().
     *
     * @param objects arbitrary number of objects.
     * @return a List from the objects param
     */
    public static <T> List<T> list(T... objects) {
        return new ArrayList<>(Arrays.asList(objects));
    }

    // get around empty constructors complaining about zero-arg varargs calls
    // like the one above when the list content
    // uses generics

    /**
     * (See {@link #list(Object[])} for documentation)
     */
    public static <T> List<T> list() {
        return new ArrayList<>();
    }

    public static <T> List<T> list(Collection<T> someCollection) {
        return new ArrayList<>(someCollection);
    }

    public static <T> List<T> list(Iterable<T> things) {
        // Why isn't this in Collections somewhere?
        return list(things.iterator());
    }

    public static <T> List<T> list(Iterator<T> things) {
        // Why isn't this in Collections somewhere?
        List<T> result = new ArrayList<>();
        while (things.hasNext()) {
            result.add(things.next());
        }
        return result;
    }

    /**
     * Just like Py.list(), but it returns a Set.
     *
     * @param objects arbitrary number of objects.
     * @return a Set from the objects param
     */
    public static <T> PySet<T> set(T... objects) {
        return set(Arrays.asList(objects));
    }

    public static <T> PySet<T> set(Collection<T> someCollection) {
        return new PySet<>(someCollection);
    }

    /**
     * Like Py.list(), but returns an immutable sequence.
     * <p>
     * Named after the same concept in Python, which in turn was named after the
     * pre-existing mathematical concept of a "tuple"
     *
     * @param objects arbitrary number of objects.
     * @return an immutible List
     */
    public static <T> List<T> tuple(T... objects) {
        return Collections.unmodifiableList(list(objects));
    }

    public static <T> List<T> tuple(List<T> someList) {
        return Collections.unmodifiableList(someList);
    }

    /**
     * Just like Py.set(), but it returns an immutable Set.
     * <p>
     * Called "frozen"set because that's what Python calls it - might as well,
     * given the context and purpose of this class.
     *
     * @param objects arbitrary number of objects.
     * @return an immutable Set from the objects param
     */
    public static <T> Set<T> frozenset(T... objects) {
        return Collections.unmodifiableSet(set(objects));
    }

    public static <T> Set<T> frozenset(Set<T> someSet) {
        return Collections.unmodifiableSet(someSet);
    }

//    // Python : dict :: Java : Map
//    public static <K, V> Map<K, V> map(Map<K, V> otherMap) {
//        return new ConcurrentHashMap<>(otherMap);
//    }

    public static <K, V> Map<K, V> map(List<K> keys, List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("key and value lists MUST be the same size!");
        }

        Map<K, V> newMap = map();
        for (int i = 0; i < keys.size(); i++) {
            newMap.put(keys.get(i), values.get(i));
        }

        return newMap;
    }

    // returns an initialized but empty Map.
    public static <K, V> Map<K, V> map() {
        return new ArrayMap<>();
    }

//    public static <K, V> Map<K, V> frozenmap(Map<K, V> otherMap) {
//        return Collections.unmodifiableMap(map(otherMap));
//    }

    public static <K, V> Map<K, V> frozenmap(List<K> keys, List<V> values) {
        return Collections.unmodifiableMap(map(keys, values));
    }

    // returns an initialized but empty Map.
    public static <K, V> Map<K, V> frozenmap() {
        return Collections.unmodifiableMap(new ArrayMap<K, V>());
    }

    /**
     * A Set implementation that offers an interface similar to Python's set
     * objects, which offer methods for the simple, standard terms of set theory
     * (e.g.: "union", "intersection", etc).
     * <p>
     * These methods all create new sets instead of modifying existing ones in
     * place. In keeping with the theme of this class, this is similar to what
     * the Python methods of the same name will do.
     * <p>
     * This is different than what Java's sets do, but Java's sets use different
     * method names which match their behavior, so I believe this shouldn't
     * cause anyone to trip up.
     *
     * @param <T>
     * @author jknutson
     */
    public static class PySet<T> extends LinkedHashSet<T> {

        private static final long serialVersionUID = 2423791518942099628L;

        public PySet(Collection<T> other) {
            super(other);
        }

        /**
         * Return a new set with elements from this set and all elements from
         * <code>others</code>.
         *
         * @param others , one or more collections with
         * @return (see above)
         */
        public PySet<T> getUnion(Collection<T>... others) {
            PySet<T> newCopy = set(this);
            for (Collection<T> other : others) {
                newCopy.addAll(other);
            }

            return newCopy;
        }

        /**
         * Return a new set with elements common to this set and all elements
         * from <code>others</code>.
         * <p>
         * This method is separate from {{@link #getIntersection(Collection...)}
         * because until Java 1.7's SafeVarargs annotation, there was no way to
         * call a varargs method using generic collections without getting a
         * (bogus) type safety error.
         */
        public PySet<T> getIntersection(Collection<T> other) {
            PySet<T> newCopy = set(this);
            // NOTE: .retainAll() can be thought of as .retainOnly() or
            // "remove all except these".
            // Javadoc: http: // goo.gl/HF6vn
            newCopy.retainAll(other);
            return newCopy;
        }

        /**
         * Return a new set with elements common to this set and all elements
         * from <code>others</code>.
         *
         * @param others
         * @return
         */
        public PySet<T> getIntersection(Collection<T>... others) {
            PySet<T> newCopy = set(this);
            for (Collection<T> other : others) {
                // NOTE: .retainAll() can be thought of as .retainOnly() or
                // "remove all except these".
                // Javadoc: http: // goo.gl/HF6vn
                newCopy.retainAll(other);
            }

            return newCopy;
        }

        /**
         * Return a new set with elements in this set which do not exist in
         * <code>other</code>.
         *
         * @param other
         * @return
         */
        public PySet<T> getDifference(Collection<T> other) {
            // Return a new set with elements in the set that are not in the
            // others.
            PySet<T> newCopy = set(this);
            newCopy.removeAll(other);
            return newCopy;
        }

        /**
         * Return a new set with elements in this set which do not exist in any
         * of the <code>others</code>.
         *
         * @param others
         * @return
         */
        public PySet<T> getDifference(Collection<T>... others) {
            // Return a new set with elements in the set that are not in the
            // others.
            PySet<T> newCopy = set(this);
            for (Collection<T> other : others) {
                newCopy.removeAll(other);
            }

            return newCopy;
        }

        /**
         * Return a new set with elements in this set which do not exist in any
         * of the <code>others</code>.
         *
         * @param other
         * @return
         */
        public PySet<T> getSymmetricDifference(Collection<T>... others) {
            // Return a new set with elements in either the set or other but not
            // both.
            PySet<T> union = set(this);
            for (Collection<T> other : others) {
                union.addAll(other);
            }
            PySet<T> intersection = set(this);
            for (Collection<T> other : others) {
                if (intersection.isEmpty()) {
                    // Don't do any more work if the intersection can't be
                    // anything other than an empty set.
                    break;
                }
                intersection = intersection.getIntersection(other);
            }
            return union.getDifference(intersection);
        }
    }

}