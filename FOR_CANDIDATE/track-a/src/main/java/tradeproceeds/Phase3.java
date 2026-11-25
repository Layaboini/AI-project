package tradeproceeds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 3: load base trades and events, rebuild state, answer top-accounts. */
public final class Phase3 {
    private Phase3() {}

    public static String toJson(List<TopAccountsQuery.AccountResult> rows) throws IOException {
        List<Map<String, Object>> out = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("account_id", r.accountId());
            m.put("net_value", r.netValue());
            m.put("trade_count", r.tradeCount());
            return m;
        }).toList();
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(out);
    }

    public static String topAccounts(Path tradesPath, Path eventsDir, String start, String end,
                                     int limit, int offset) throws IOException {
        TopAccountsQuery.validate(start, end, limit, offset);
        TradeCsvParser.ParseResult base = TradeCsvParser.parseFile(tradesPath);
        Map<String, Trade> state = StateBuilder.build(base.trades(), EventLoader.loadDirectory(eventsDir));
        return toJson(TopAccountsQuery.run(state.values(), start, end, limit, offset));
    }
}
