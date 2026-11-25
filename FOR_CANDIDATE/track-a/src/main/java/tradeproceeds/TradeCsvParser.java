package tradeproceeds;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Parses trades.csv. See DECISIONS.md for what counts as malformed. */
public final class TradeCsvParser {
    private TradeCsvParser() {}

    public record ParseResult(List<Trade> trades, int skipped) {}

    private static final Set<String> VALID_STATUSES =
            Set.of("filled", "partially_filled", "busted", "cancelled");

    public static ParseResult parseFile(Path path) throws IOException {
        return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    public static ParseResult parse(List<String> lines) {
        List<Trade> trades = new ArrayList<>();
        int skipped = 0;
        boolean first = true;
        for (String raw : lines) {
            String line = raw.trim();
            if (first) {
                first = false;
                if (line.startsWith("trade_id")) continue; // header
            }
            if (line.isEmpty()) continue; // blank lines are not rows
            Trade t = parseRow(line);
            if (t == null) skipped++;
            else trades.add(t);
        }
        return new ParseResult(trades, skipped);
    }

    /** Returns null when the row is malformed. */
    public static Trade parseRow(String line) {
        String[] cols = line.split(",", -1);
        if (cols.length != 6) return null;
        String id = cols[0].trim();
        String account = cols[1].trim();
        String status = cols[4].trim();
        String date = cols[5].trim();
        if (id.isEmpty() || account.isEmpty()) return null;
        if (!VALID_STATUSES.contains(status)) return null;
        if (!isDate(date)) return null;
        int qty;
        BigDecimal price;
        try {
            qty = Integer.parseInt(cols[2].trim());
            price = new BigDecimal(cols[3].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        if (qty < 0 || price.signum() < 0) return null;

        Trade t = new Trade();
        t.tradeId = id;
        t.accountId = account;
        t.quantity = qty;
        t.price = price;
        t.status = status;
        t.tradeDate = date;
        return t;
    }

    public static boolean isValidStatus(String s) {
        return s != null && VALID_STATUSES.contains(s);
    }

    public static boolean isDate(String s) {
        if (s == null || s.length() != 10) return false;
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
