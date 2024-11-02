package eu.ciechanowiec.sling.rocket.jcr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Represents default {@link Property}-ies of a {@link Node}.
 */
@SuppressWarnings({"MagicNumber", "WeakerAccess", "ClassWithTooManyFields"})
public final class DefaultProperties {

    /**
     * Default value for {@link PropertyType#STRING}.
     */
    public static final String STRING_EMPTY = StringUtils.EMPTY;

    /**
     * Java {@link Class} that represents {@link PropertyType#STRING}.
     */
    public static final Class<String> STRING_CLASS = String.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#STRING}.
     */
    public static final Class<String[]> STRING_CLASS_ARRAY = String[].class;

    /**
     * Default value for {@link PropertyType#BOOLEAN}.
     */
    public static final boolean BOOLEAN_FALSE = Boolean.FALSE;

    /**
     * Java {@link Class} that represents {@link PropertyType#BOOLEAN}.
     */
    public static final Class<Boolean> BOOLEAN_CLASS = Boolean.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#BOOLEAN}.
     */
    public static final Class<boolean[]> BOOLEAN_CLASS_ARRAY = boolean[].class;

    /**
     * Default value for {@link PropertyType#LONG}.
     */
    public static final long LONG_ZERO = NumberUtils.LONG_ZERO;

    /**
     * Java {@link Class} that represents {@link PropertyType#LONG}.
     */
    public static final Class<Long> LONG_CLASS = Long.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#LONG}.
     */
    public static final Class<long[]> LONG_CLASS_ARRAY = long[].class;

    /**
     * Default value for {@link PropertyType#DOUBLE}.
     */
    public static final double DOUBLE_ZERO = NumberUtils.DOUBLE_ZERO;

    /**
     * Java {@link Class} that represents {@link PropertyType#DOUBLE}.
     */
    public static final Class<Double> DOUBLE_CLASS = Double.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#DOUBLE}.
     */
    public static final Class<double[]> DOUBLE_CLASS_ARRAY = double[].class;

    /**
     * Default value for {@link PropertyType#DECIMAL}.
     */
    public static final BigDecimal DECIMAL_ZERO = new BigDecimal(BigInteger.ZERO);

    /**
     * Java {@link Class} that represents {@link PropertyType#DECIMAL}.
     */
    public static final Class<BigDecimal> DECIMAL_CLASS = BigDecimal.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#DECIMAL}.
     */
    public static final Class<BigDecimal[]> DECIMAL_CLASS_ARRAY = BigDecimal[].class;

    /**
     * Default value for {@link PropertyType#DATE}.
     */
    @SuppressWarnings("squid:S2885")
    @SuppressFBWarnings("STCAL_STATIC_CALENDAR_INSTANCE")
    public static final Calendar DATE_UNIX_EPOCH;

    /**
     * Java {@link Class} that represents {@link PropertyType#DATE}.
     */
    public static final Class<Calendar> DATE_CLASS = Calendar.class;

    /**
     * Java {@link Class} that represents arrayed {@link PropertyType#DATE}.
     */
    public static final Class<Calendar[]> DATE_CLASS_ARRAY = Calendar[].class;

    static {
        DATE_UNIX_EPOCH = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        DATE_UNIX_EPOCH.clear();
        DATE_UNIX_EPOCH.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        DATE_UNIX_EPOCH.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Blocks construction of an instance of this class.
     */
    private DefaultProperties() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#STRING}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#STRING}
     */
    public static String of(String defaultValue) {
        return defaultValue;
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#BOOLEAN}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#BOOLEAN}
     */
    public static boolean of(boolean defaultValue) {
        return defaultValue;
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#LONG}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#LONG}
     */
    public static long of(long defaultValue) {
        return defaultValue;
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#DOUBLE}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#DOUBLE}
     */
    public static double of(double defaultValue) {
        return defaultValue;
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#DECIMAL}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#DECIMAL}
     */
    public static BigDecimal of(BigDecimal defaultValue) {
        return defaultValue;
    }

    /**
     * Creates a value of a {@link Node} property of type {@link PropertyType#DATE}.
     * @param defaultValue content of the constructed value
     * @return value of a {@link Node} property of type {@link PropertyType#DATE}
     */
    public static Calendar of(Calendar defaultValue) {
        return defaultValue;
    }
}
