package tradeproceeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Phase 1: proceeds per account from filled trades only. */
public final class Phase1 {
    private Phase1() {}

    public record AccountProceeds(String accountId, int tradeCount, BigDecimal proceeds) {}

    public static List<AccountProceeds> compute(List<Trade> trades) {
        Map<String, BigDecimal> sums = new TreeMap<>();
        Map<String, Integer> counts = new TreeMap<>();
        for (Trade t : trades) {
            if (!"filled".equals(t.status)) continue;
            sums.merge(t.accountId, Notional.of(t), BigDecimal::add);
            counts.merge(t.accountId, 1, Integer::sum);
        }
        return sums.entrySet().stream()
                .map(e -> new AccountProceeds(e.getKey(), counts.get(e.getKey()), Notional.round(e.getValue())))
                .sorted(Comparator.comparing(AccountProceeds::proceeds).reversed()
                        .thenComparing(AccountProceeds::accountId))
                .toList();
    }

    public static String toJson(List<AccountProceeds> rows) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", r.accountId());
            m.put("trade_count", r.tradeCount());
            m.put("proceeds", r.proceeds());
            return m;
        }).toList();
        return mapper.writeValueAsString(out);
    }

    public static void run(Path tradesPath, Path outPath) throws IOException {
        TradeCsvParser.ParseResult parsed = TradeCsvParser.parseFile(tradesPath);
        System.err.println(parsed.skipped());
        Path parent = outPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(outPath, toJson(compute(parsed.trades())));
    }
}
