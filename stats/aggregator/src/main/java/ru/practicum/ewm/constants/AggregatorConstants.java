package ru.practicum.ewm.constants;

import java.math.BigDecimal;
import java.math.MathContext;

public final class AggregatorConstants {
    public static final BigDecimal VIEW_WEIGHT = BigDecimal.valueOf(0.4);
    public static final BigDecimal REGISTER_WEIGHT = BigDecimal.valueOf(0.8);
    public static final BigDecimal LIKE_WEIGHT = BigDecimal.valueOf(1.0);
    public static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private AggregatorConstants() {
    }
}