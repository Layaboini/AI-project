package tradeproceeds;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rebuilds current trade state from base trades plus events.
 * Ordering: timestamp (parsed as an instant) ascending, then event_id ascending.
 * Idempotency: after sorting, only the first occurrence of each event_id is applied.
 */
public final class StateBuilder {
    private StateBuilder() {}

    private record Stamped(TradeEvent event, Instant at) {}

    public static Map<String, Trade> build(List<Trade> baseTrades, List<TradeEvent> events) {
        Map<String, Trade> state = new LinkedHashMap<>();
        for (Trade t : baseTrades) state.put(t.tradeId, t.copy());

        List<Stamped> ordered = new ArrayList<>();
        for (TradeEvent e : events) {
            Instant at = parseInstant(e.timestamp);
            if (at == null || e.eventId == null || e.type == null || e.tradeId == null) continue;
            ordered.add(new Stamped(e, at));
        }
        ordered.sort(Comparator.comparing(Stamped::at).thenComparing(s -> s.event().eventId));

        Set<String> seen = new HashSet<>();
        for (Stamped s : ordered) {
            if (!seen.add(s.event().eventId)) continue; // duplicate delivery
            apply(state, s.event());
        }
        return state;
    }

    private static void apply(Map<String, Trade> state, TradeEvent e) {
        switch (e.type) {
            case "new" -> applyNew(state, e);
            case "amend" -> applyAmend(state, e);
            case "cancel" -> setStatus(state, e.tradeId, "cancelled");
            case "bust" -> setStatus(state, e.tradeId, "busted");
            default -> { }
        }
    }

    private static void setStatus(Map<String, Trade> state, String tradeId, String status) {
        Trade t = state.get(tradeId);
        if (t != null) t.status = status;
    }

    /** A "new" event fully replaces any existing trade. Incomplete or invalid ones are ignored. */
    private static void applyNew(Map<String, Trade> state, TradeEvent e) {
        if (e.accountId == null || e.accountId.isBlank() || e.quantity == null || e.quantity < 0
                || e.price == null || e.price.signum() < 0
                || e.status == null || !TradeCsvParser.isValidStatus(e.status)
                || !TradeCsvParser.isDate(e.tradeDate)) return;
        Trade t = new Trade();
        t.tradeId = e.tradeId;
        t.accountId = e.accountId;
        t.quantity = e.quantity;
        t.price = e.price;
        t.status = e.status;
        t.tradeDate = e.tradeDate;
        state.put(e.tradeId, t);
    }

    /** Sets only the provided fields; the whole amend is ignored if any provided value is invalid. */
    private static void applyAmend(Map<String, Trade> state, TradeEvent e) {
        Trade t = state.get(e.tradeId);
        if (t == null) return;
        if (e.quantity != null && e.quantity < 0) return;
        if (e.price != null && e.price.signum() < 0) return;
        if (e.status != null && !TradeCsvParser.isValidStatus(e.status)) return;
        if (e.tradeDate != null && !TradeCsvParser.isDate(e.tradeDate)) return;
        if (e.quantity != null) t.quantity = e.quantity;
        if (e.price != null) t.price = e.price;
        if (e.status != null) t.status = e.status;
        if (e.tradeDate != null) t.tradeDate = e.tradeDate;
    }

    private static Instant parseInstant(String s) {
        if (s == null) return null;
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
