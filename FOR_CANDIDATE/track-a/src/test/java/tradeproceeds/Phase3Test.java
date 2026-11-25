package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase3Test {
    private static TradeEvent ev(String id, String type, String ts, String tradeId) {
        TradeEvent e = new TradeEvent();
        e.eventId = id;
        e.type = type;
        e.timestamp = ts;
        e.tradeId = tradeId;
        return e;
    }

    private static TradeEvent newEv(String id, String ts, String tradeId, String acct, int qty, String price, String status) {
        TradeEvent e = ev(id, "new", ts, tradeId);
        e.accountId = acct;
        e.quantity = qty;
        e.price = new BigDecimal(price);
        e.status = status;
        e.tradeDate = "2024-05-02";
        return e;
    }

    private static List<Trade> base() {
        return TradeCsvParser.parse(List.of("Z1,A001,2,50.00,filled,2024-05-01")).trades();
    }

    @Test
    void workedExampleMatchesExpectedOutput() throws Exception {
        List<Trade> b = TradeCsvParser.parseFile(Path.of("../examples/phase-3-base-trades.csv")).trades();
        List<TradeEvent> events = EventLoader.loadFile(Path.of("../examples/phase-3-events.json"));
        Map<String, Trade> state = StateBuilder.build(b, events);
        String json = Phase3.toJson(TopAccountsQuery.run(state.values(), "2024-05-01", "2024-05-31", 10, 0));

        ObjectMapper m = new ObjectMapper();
        JsonNode actual = m.readTree(json);
        JsonNode expected = m.readTree(Files.readString(Path.of("../examples/phase-3-query-output.json")));
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).get("account_id").asText(), actual.get(i).get("account_id").asText());
            assertEquals(expected.get(i).get("trade_count").asInt(), actual.get(i).get("trade_count").asInt());
            assertEquals(0, expected.get(i).get("net_value").decimalValue()
                    .compareTo(actual.get(i).get("net_value").decimalValue()));
        }
    }

    @Test
    void eventsApplyInTimestampOrderRegardlessOfInputOrder() {
        TradeEvent amend = ev("E2", "amend", "2024-05-03T09:00:00Z", "Z1");
        amend.quantity = 3;
        TradeEvent cancel = ev("E1", "cancel", "2024-05-04T09:00:00Z", "Z1");
        List<TradeEvent> events = new ArrayList<>(List.of(cancel, amend));
        Map<String, Trade> a = StateBuilder.build(base(), events);
        Collections.reverse(events);
        Map<String, Trade> b = StateBuilder.build(base(), events);
        assertEquals("cancelled", a.get("Z1").status);
        assertEquals(3, a.get("Z1").quantity);
        assertEquals(a.get("Z1").status, b.get("Z1").status);
    }

    @Test
    void equalTimestampsBreakTiesByEventId() {
        TradeEvent bust = ev("B", "bust", "2024-05-03T09:00:00Z", "Z1");
        TradeEvent amend = ev("A", "amend", "2024-05-03T09:00:00Z", "Z1");
        amend.status = "filled";
        Map<String, Trade> s = StateBuilder.build(base(), List.of(bust, amend));
        assertEquals("busted", s.get("Z1").status);
    }

    @Test
    void duplicateEventIdsAreAppliedOnce() {
        TradeEvent f1 = newEv("N1", "2024-05-02T09:00:00Z", "Z2", "A002", 1, "200", "filled");
        TradeEvent f2 = newEv("N1", "2024-05-02T09:00:00Z", "Z2", "A002", 1, "200", "filled");
        TradeEvent amendA = ev("M1", "amend", "2024-05-03T09:00:00Z", "Z1");
        amendA.quantity = 9;
        TradeEvent amendB = ev("M1", "amend", "2024-05-05T09:00:00Z", "Z1");
        amendB.quantity = 1;
        Map<String, Trade> s = StateBuilder.build(base(), List.of(f1, f2, amendB, amendA));
        assertEquals(9, s.get("Z1").quantity);
        assertEquals(1, s.get("Z2").quantity);
    }

    @Test
    void amendOfUnknownTradeIsIgnoredAndNewReplaces() {
        TradeEvent amend = ev("E1", "amend", "2024-05-02T09:00:00Z", "NOPE");
        amend.quantity = 5;
        TradeEvent replace = newEv("E2", "2024-05-03T09:00:00Z", "Z1", "A009", 1, "1", "filled");
        Map<String, Trade> s = StateBuilder.build(base(), List.of(amend, replace));
        assertEquals(1, s.size());
        assertEquals("A009", s.get("Z1").accountId);
    }

    @Test
    void paginationAndRanking() {
        List<Trade> t = TradeCsvParser.parse(List.of(
                "T1,A,1,10,filled,2024-05-01", "T2,B,1,30,filled,2024-05-01",
                "T3,C,1,30,filled,2024-05-01", "T4,D,1,5,filled,2024-06-01")).trades();
        List<TopAccountsQuery.AccountResult> r = TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 2, 1);
        assertEquals(List.of("C", "A"), r.stream().map(TopAccountsQuery.AccountResult::accountId).toList());
    }

    @Test
    void invalidParametersAreRejectedWithClearMessages() {
        List<Trade> t = List.of();
        assertThrows(TopAccountsQuery.InvalidQueryException.class, () -> TopAccountsQuery.run(t, "2024-13-01", "2024-05-31", 1, 0));
        assertThrows(TopAccountsQuery.InvalidQueryException.class, () -> TopAccountsQuery.run(t, "2024-06-01", "2024-05-31", 1, 0));
        assertThrows(TopAccountsQuery.InvalidQueryException.class, () -> TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 0, 0));
        assertThrows(TopAccountsQuery.InvalidQueryException.class, () -> TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 1, -1));
    }
}
