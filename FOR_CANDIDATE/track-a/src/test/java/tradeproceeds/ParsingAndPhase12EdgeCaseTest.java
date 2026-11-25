package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParsingAndPhase12EdgeCaseTest {
    private static List<Trade> trades(String... rows) {
        return TradeCsvParser.parse(List.of(rows)).trades();
    }

    @Test
    void headerOnlyAndBlankFilesProduceNothingAndSkipNothing() {
        TradeCsvParser.ParseResult r = TradeCsvParser.parse(List.of(
                "trade_id,account_id,quantity,price,status,trade_date", "", "   "));
        assertTrue(r.trades().isEmpty());
        assertEquals(0, r.skipped());
        assertEquals(0, TradeCsvParser.parse(List.of()).skipped());
    }

    @Test
    void whitespaceAroundFieldsIsTrimmed() {
        List<Trade> t = trades(" T1 , A1 , 2 , 1.50 , filled , 2024-01-01 ");
        assertEquals(1, t.size());
        assertEquals("A1", t.get(0).accountId);
        assertEquals(2, t.get(0).quantity);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "T1,A1,-1,1.0,filled,2024-01-01",
            "T1,A1,2,-1.0,filled,2024-01-01",
            "T1,A1,2.5,1.0,filled,2024-01-01",
            "T1,A1,2,1.0,filled,2024-01-01,extra",
            "T1,A1,2,1.0,FILLED,2024-01-01",
            "T1,A1,2,1.0,filled,2024-02-30",
            "T1,A1,2,1.0,filled,2024-1-1",
            "T1,A1,99999999999,1.0,filled,2024-01-01"})
    void moreMalformedShapesAreSkipped(String row) {
        TradeCsvParser.ParseResult r = TradeCsvParser.parse(List.of(row));
        assertTrue(r.trades().isEmpty());
        assertEquals(1, r.skipped());
    }

    @Test
    void leapDayIsAValidDate() {
        assertEquals(1, trades("T1,A1,1,1.0,filled,2024-02-29").size());
        assertEquals(0, trades("T1,A1,1,1.0,filled,2023-02-29").size());
    }

    @Test
    void phase1RoundsHalfUpAndKeepsZeroQuantityFilledAccounts() {
        List<Phase1.AccountProceeds> rows = Phase1.compute(trades(
                "T1,A,1,0.125,filled,2024-01-01", "T2,Z,0,10,filled,2024-01-01"));
        assertEquals("0.13", rows.get(0).proceeds().toPlainString());
        assertEquals("Z", rows.get(1).accountId());
        assertEquals("0.00", rows.get(1).proceeds().toPlainString());
    }

    @Test
    void phase1IgnoresEverythingExceptFilled() {
        assertTrue(Phase1.compute(trades(
                "T1,A,1,10,partially_filled,2024-01-01", "T2,A,1,10,busted,2024-01-01",
                "T3,A,1,10,cancelled,2024-01-01")).isEmpty());
    }

    @Test
    void phase2MonthBoundariesAndBustOnlyGroupsGoNegative() {
        List<Phase2.Row> rows = Phase2.compute(trades(
                "T1,A,1,10,filled,2024-01-31", "T2,A,1,10,filled,2024-02-01",
                "T3,B,3,5,busted,2024-01-15"), Map.of("A", "US", "B", "DE"));
        assertEquals(3, rows.size());
        assertEquals("DE", rows.get(0).region());
        assertEquals("-15.00", rows.get(0).netValue().toPlainString());
        assertEquals("2024-01", rows.get(1).month());
        assertEquals("2024-02", rows.get(2).month());
    }

    @Test
    void phase2PartiallyFilledCountsAndCancelledOnlyGroupIsAbsent() {
        List<Phase2.Row> rows = Phase2.compute(trades(
                "T1,A,2,10,partially_filled,2024-01-01", "T2,B,5,10,cancelled,2024-01-01"),
                Map.of("A", "US", "B", "DE"));
        assertEquals(1, rows.size());
        assertEquals("US", rows.get(0).region());
        assertEquals(1, rows.get(0).tradeCount());
    }

    @Test
    void phase2CsvHasHeaderAndTwoDecimalPlaces() {
        String csv = Phase2.toCsv(Phase2.compute(trades("T1,A,1,10,filled,2024-01-01"), Map.of()));
        assertEquals("region,month,trade_count,net_value\nUNKNOWN,2024-01,1,10.00\n", csv);
    }
}
