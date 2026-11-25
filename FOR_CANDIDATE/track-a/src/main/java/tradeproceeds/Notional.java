package tradeproceeds;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared value rules, reused by every phase. */
public final class Notional {
    private Notional() {}

    public static BigDecimal of(Trade t) {
        return t.price.multiply(BigDecimal.valueOf(t.quantity));
    }

    /** Phase 2+ net contribution: filled/partial add, busted subtracts, cancelled is zero. */
    public static BigDecimal net(Trade t) {
        switch (t.status) {
            case "filled":
            case "partially_filled":
                return of(t);
            case "busted":
                return of(t).negate();
            default:
                return BigDecimal.ZERO;
        }
    }

    /** Cancelled trades are excluded entirely from Phase 2+ reports. */
    public static boolean counts(Trade t) {
        return !"cancelled".equals(t.status);
    }

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
