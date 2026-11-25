package tradeproceeds;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Phase 2: monthly net traded value grouped by region. */
public final class Phase2 {
    private Phase2() {}

    public static final String UNKNOWN_REGION = "UNKNOWN";

    public record Row(String region, String month, int tradeCount, BigDecimal netValue) {}

    private static final class Agg {
        int count;
        BigDecimal net = BigDecimal.ZERO;
    }

    /** Trades are grouped by (region, month); TreeMap gives region then month ordering. */
    public static List<Row> compute(List<Trade> trades, Map<String, String> regions) {
        Map<String, Map<String, Agg>> grouped = new TreeMap<>();
        for (Trade t : trades) {
            if (!Notional.counts(t)) continue;
            String region = regions.getOrDefault(t.accountId, UNKNOWN_REGION);
            String month = t.tradeDate.substring(0, 7);
            Agg a = grouped.computeIfAbsent(region, r -> new TreeMap<>()).computeIfAbsent(month, m -> new Agg());
            a.count++;
            a.net = a.net.add(Notional.net(t));
        }
        List<Row> rows = new ArrayList<>();
        grouped.forEach((region, months) -> months.forEach((month, a) ->
                rows.add(new Row(region, month, a.count, Notional.round(a.net)))));
        return rows;
    }

    public static String toCsv(List<Row> rows) {
        StringBuilder sb = new StringBuilder("region,month,trade_count,net_value\n");
        for (Row r : rows) {
            sb.append(r.region()).append(',').append(r.month()).append(',')
              .append(r.tradeCount()).append(',').append(r.netValue().toPlainString()).append('\n');
        }
        return sb.toString();
    }

    public static void run(Path tradesPath, Path accountsPath, Path outPath) throws IOException {
        TradeCsvParser.ParseResult parsed = TradeCsvParser.parseFile(tradesPath);
        System.err.println(parsed.skipped());
        Map<String, String> regions = AccountCsvParser.parseFile(accountsPath);
        Path parent = outPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(outPath, toCsv(compute(parsed.trades(), regions)), StandardCharsets.UTF_8);
    }
}
