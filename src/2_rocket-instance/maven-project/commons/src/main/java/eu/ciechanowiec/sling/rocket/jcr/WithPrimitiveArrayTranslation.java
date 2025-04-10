package eu.ciechanowiec.sling.rocket.jcr;

import java.util.Optional;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.ValueMap;

class WithPrimitiveArrayTranslation {

    private final ValueMap valueMap;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    WithPrimitiveArrayTranslation(ValueMap valueMap) {
        this.valueMap = valueMap;
    }

    @SuppressWarnings(
        {
            "ReturnCount", "MethodWithMultipleReturnPoints", "IfCanBeSwitch",
            "IfStatementWithTooManyBranches", "ChainOfInstanceofChecks"
        }
    )
    <T> T get(String propertyName, T defaultValue) {
        if (defaultValue instanceof boolean[] defaultValueCast) {
            Boolean[] defaultValueObj = ArrayUtils.toObject(defaultValueCast);
            Boolean[] extractedValues = valueMap.get(propertyName, defaultValueObj);
            return (T) ArrayUtils.toPrimitive(extractedValues);
        } else if (defaultValue instanceof long[] defaultValueCast) {
            Long[] defaultValueObj = ArrayUtils.toObject(defaultValueCast);
            Long[] extractedValues = valueMap.get(propertyName, defaultValueObj);
            return (T) ArrayUtils.toPrimitive(extractedValues);
        } else if (defaultValue instanceof double[] defaultValueCast) {
            Double[] defaultValueObj = ArrayUtils.toObject(defaultValueCast);
            Double[] extractedValues = valueMap.get(propertyName, defaultValueObj);
            return (T) ArrayUtils.toPrimitive(extractedValues);
        } else {
            return valueMap.get(propertyName, defaultValue);
        }
    }

    @SuppressWarnings(
        {
            "ReturnCount", "MethodWithMultipleReturnPoints", "IfCanBeSwitch", "IfStatementWithTooManyBranches"
        }
    )
    <T> Optional<T> get(String propertyName, Class<T> type) {
        if (type.equals(boolean[].class)) {
            return Optional.ofNullable(valueMap.get(propertyName, Boolean[].class))
                .map(ArrayUtils::toPrimitive)
                .map(array -> (T) array);
        } else if (type.equals(long[].class)) {
            return Optional.ofNullable(valueMap.get(propertyName, Long[].class))
                .map(ArrayUtils::toPrimitive)
                .map(array -> (T) array);
        } else if (type.equals(double[].class)) {
            return Optional.ofNullable(valueMap.get(propertyName, Double[].class))
                .map(ArrayUtils::toPrimitive)
                .map(array -> (T) array);
        } else {
            return Optional.ofNullable(valueMap.get(propertyName, type));
        }
    }
}
