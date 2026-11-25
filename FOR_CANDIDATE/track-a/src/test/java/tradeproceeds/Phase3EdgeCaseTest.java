package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase3EdgeCaseTest {
    private static TradeEvent ev(String id, String type, String ts, String tradeId) {
        TradeEvent e = new TradeEvent();
        e.eventId = id;
        e.type = type;
        e.timestamp = ts;
        e.tradeId = tradeId;
        return e;
    }

    private static List<Trade> base() {
        return TradeCsvParser.parse(List.of("Z1,A001,2,50.00,filled,2024-05-01")).trades();
    }

    @Test
    void timestampsWithOffsetsAreOrderedAsInstantsNotText() {
        // 10:00+02:00 is 08:00Z, so it happens BEFORE 09:00Z even though its text sorts later.
        TradeEvent early = ev("E2", "amend", "2024-05-03T10:00:00+02:00", "Z1");
        early.quantity = 7;
        TradeEvent late = ev("E1", "amend", "2024-05-03T09:00:00Z", "Z1");
        late.quantity = 9;
        Map<String, Trade> s = StateBuilder.build(base(), List.of(early, late));
        assertEquals(9, s.get("Z1").quantity);
    }

    @Test
    void fractionalSecondsOrderCorrectly() {
        TradeEvent a = ev("A", "amend", "2024-05-03T09:00:00.500Z", "Z1");
        a.quantity = 1;
        TradeEvent b = ev("B", "amend", "2024-05-03T09:00:00.250Z", "Z1");
        b.quantity = 2;
        assertEquals(1, StateBuilder.build(base(), List.of(a, b)).get("Z1").quantity);
    }

    @Test
    void earlierRedeliveryOfAnEventIdWins() {
        TradeEvent laterCopy = ev("M1", "amend", "2024-05-05T09:00:00Z", "Z1");
        laterCopy.quantity = 1;
        TradeEvent earlierCopy = ev("M1", "amend", "2024-05-03T09:00:00Z", "Z1");
        earlierCopy.quantity = 9;
        assertEquals(9, StateBuilder.build(base(), List.of(laterCopy, earlierCopy)).get("Z1").quantity);
    }

    @Test
    void malformedEventsAreSkippedWithoutCrashing() {
        TradeEvent badTs = ev("E1", "bust", "not-a-time", "Z1");
        TradeEvent noId = ev(null, "bust", "2024-05-03T09:00:00Z", "Z1");
        TradeEvent noTrade = ev("E3", "bust", "2024-05-03T09:00:00Z", null);
        TradeEvent unknownType = ev("E4", "explode", "2024-05-03T09:00:00Z", "Z1");
        Map<String, Trade> s = StateBuilder.build(base(), List.of(badTs, noId, noTrade, unknownType));
        assertEquals("filled", s.get("Z1").status);
    }

    @Test
    void amendWithAnyInvalidFieldIsIgnoredEntirely() {
        TradeEvent amend = ev("E1", "amend", "2024-05-03T09:00:00Z", "Z1");
        amend.quantity = 99;
        amend.status = "settled";
        assertEquals(2, StateBuilder.build(base(), List.of(amend)).get("Z1").quantity);
    }

    @Test
    void amendCanReviveACancelledTradeButCancelOfUnknownIsIgnored() {
        TradeEvent cancel = ev("E1", "cancel", "2024-05-02T09:00:00Z", "Z1");
        TradeEvent revive = ev("E2", "amend", "2024-05-03T09:00:00Z", "Z1");
        revive.status = "filled";
        TradeEvent ghost = ev("E3", "cancel", "2024-05-04T09:00:00Z", "NOPE");
        Map<String, Trade> s = StateBuilder.build(base(), List.of(cancel, revive, ghost));
        assertEquals("filled", s.get("Z1").status);
        assertEquals(1, s.size());
    }

    @Test
    void incompleteNewEventIsIgnoredAndDoesNotReplace() {
        TradeEvent partial = ev("E1", "new", "2024-05-02T09:00:00Z", "Z1");
        partial.accountId = "A009";
        Map<String, Trade> s = StateBuilder.build(base(), List.of(partial));
        assertEquals("A001", s.get("Z1").accountId);
    }

    @Test
    void baseListIsNotMutatedByReplay() {
        List<Trade> base = base();
        TradeEvent bust = ev("E1", "bust", "2024-05-02T09:00:00Z", "Z1");
        StateBuilder.build(base, List.of(bust));
        assertEquals("filled", base.get(0).status);
    }

    @Test
    void dateWindowIsInclusiveAndOffsetPastTheEndIsEmpty() {
        List<Trade> t = TradeCsvParser.parse(List.of(
                "T1,A,1,10,filled,2024-05-01", "T2,B,1,20,filled,2024-05-31",
                "T3,C,1,30,filled,2024-06-01", "T4,D,1,40,filled,2024-04-30")).trades();
        List<TopAccountsQuery.AccountResult> r = TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 10, 0);
        assertEquals(List.of("B", "A"), r.stream().map(TopAccountsQuery.AccountResult::accountId).toList());
        assertTrue(TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 10, 5).isEmpty());
        assertEquals(1, TopAccountsQuery.run(t, "2024-05-01", "2024-05-01", 10, 0).size());
    }

    @Test
    void busterAndCancelledTradesRankAndCountCorrectly() {
        List<Trade> t = TradeCsvParser.parse(List.of(
                "T1,A,2,10,busted,2024-05-01", "T2,B,5,10,cancelled,2024-05-01",
                "T3,C,1,1,filled,2024-05-01")).trades();
        List<TopAccountsQuery.AccountResult> r = TopAccountsQuery.run(t, "2024-05-01", "2024-05-31", 10, 0);
        assertEquals(2, r.size());
        assertEquals("C", r.get(0).accountId());
        assertEquals(new BigDecimal("-20.00"), r.get(1).netValue());
    }

    @Test
    void emptyInputsGiveEmptyState() {
        assertTrue(StateBuilder.build(List.of(), List.of()).isEmpty());
        assertTrue(TopAccountsQuery.run(List.of(), "2024-01-01", "2024-12-31", 5, 0).isEmpty());
    }
}
