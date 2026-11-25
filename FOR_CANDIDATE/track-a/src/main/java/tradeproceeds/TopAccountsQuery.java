package tradeproceeds;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** top-accounts: rank accounts by net value over trades dated within [start, end]. */
public final class TopAccountsQuery {
    private TopAccountsQuery() {}

    public record AccountResult(String accountId, BigDecimal netValue, int tradeCount) {}

    /** Thrown for invalid query parameters; the message is shown to the user as-is. */
    public static final class InvalidQueryException extends IllegalArgumentException {
        public InvalidQueryException(String message) {
            super(message);
        }
    }

    public static void validate(String start, String end, int limit, int offset) {
        if (!TradeCsvParser.isDate(start)) throw new InvalidQueryException("start must be a valid YYYY-MM-DD date");
        if (!TradeCsvParser.isDate(end)) throw new InvalidQueryException("end must be a valid YYYY-MM-DD date");
        if (start.compareTo(end) > 0) throw new InvalidQueryException("start must not be after end");
        if (limit <= 0) throw new InvalidQueryException("limit must be a positive integer");
        if (offset < 0) throw new InvalidQueryException("offset must be a non-negative integer");
    }

    public static List<AccountResult> run(Collection<Trade> state, String start, String end, int limit, int offset) {
        validate(start, end, limit, offset);
        Map<String, BigDecimal> net = new TreeMap<>();
        Map<String, Integer> count = new TreeMap<>();
        for (Trade t : state) {
            if (!Notional.counts(t)) continue;
            if (t.tradeDate.compareTo(start) < 0 || t.tradeDate.compareTo(end) > 0) continue;
            net.merge(t.accountId, Notional.net(t), BigDecimal::add);
            count.merge(t.accountId, 1, Integer::sum);
        }
        return net.entrySet().stream()
                .map(e -> new AccountResult(e.getKey(), Notional.round(e.getValue()), count.get(e.getKey())))
                .sorted(Comparator.comparing(AccountResult::netValue).reversed()
                        .thenComparing(AccountResult::accountId))
                .skip(offset)
                .limit(limit)
                .toList();
    }
}
