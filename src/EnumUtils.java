import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This has been pulled from this page:
 * https://www.stubbornjava.com/posts/java-enum-lookup-by-name-or-field-without-throwing-exceptions
 *
 * Though the author made use of some Google libraries which I don't
 * necessarily need. Thus, this is a little bit less efficient but
 * it will get the job done. Also this is mostly just a one-time
 * deal, so we don't care too much about efficiency in this case.
 */
public class EnumUtils {

    public static <T, E extends Enum<E>> Function<T, E> lookupMap(Class<E> clazz, Function<E, T> mapper) {
        @SuppressWarnings("unchecked")
        E[] emptyArray = (E[]) Array.newInstance(clazz, 0);
        return lookupMap(EnumSet.allOf(clazz).toArray(emptyArray), mapper);
    }

    public static <T, E extends Enum<E>> Function<T, E> lookupMap(E[] values, Function<E, T> mapper) {
        Map<T, E> index = new HashMap<T,E>(values.length);
        for (E value : values) {
            index.put(mapper.apply(value), value);
        }
        return (T key) -> index.get(key);
    }
}
