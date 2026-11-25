package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase2Test {
    @Test
    void workedExampleMatchesExpectedOutput() throws Exception {
        List<Trade> trades = TradeCsvParser.parseFile(Path.of("../examples/phase-2-input.csv")).trades();
        Map<String, String> regions = AccountCsvParser.parseFile(Path.of("../examples/phase-2-accounts.csv"));
        String actual = Phase2.toCsv(Phase2.compute(trades, regions));
        String expected = Files.readString(Path.of("../examples/phase-2-output.csv"));
        assertEquals(expected.replace("\r\n", "\n").trim(), actual.trim());
    }

    @Test
    void cancelledExcludedBustedNegativeUnknownRegion() {
        List<Trade> trades = TradeCsvParser.parse(List.of(
                "T1,A1,1,10,cancelled,2024-01-01",
                "T2,A1,2,10,busted,2024-01-02",
                "T3,ZZ,1,5.005,filled,2024-01-03")).trades();
        List<Phase2.Row> rows = Phase2.compute(trades, Map.of("A1", "US"));
        assertEquals(2, rows.size());
        assertEquals("UNKNOWN", rows.get(0).region());
        assertEquals("US", rows.get(1).region());
        assertEquals(1, rows.get(1).tradeCount());
        assertEquals("-20.00", rows.get(1).netValue().toPlainString());
        assertEquals("5.01", rows.get(0).netValue().toPlainString());
    }

    @Test
    void malformedAccountRowsAreSkippedAndFirstWins() {
        Map<String, String> m = AccountCsvParser.parse(List.of(
                "account_id,name,region,open_date", "A1,x,US,2023-01-01", "A1,y,DE,2023-01-01", "A2,z,,2023-01-01", "bad"));
        assertEquals(Map.of("A1", "US"), m);
    }
}
