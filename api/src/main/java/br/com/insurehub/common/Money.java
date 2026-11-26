package br.com.insurehub.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
