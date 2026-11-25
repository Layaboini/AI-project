package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Phase1Test {
    static final String HEADER = "trade_id,account_id,quantity,price,status,trade_date";

    @Test
    void workedExampleMatchesExpectedOutput() throws Exception {
        TradeCsvParser.ParseResult parsed = TradeCsvParser.parseFile(Path.of("../examples/phase-1-input.csv"));
        assertEquals(1, parsed.skipped());

        ObjectMapper m = new ObjectMapper();
        JsonNode actual = m.readTree(Phase1.toJson(Phase1.compute(parsed.trades())));
        JsonNode expected = m.readTree(Files.readString(Path.of("../examples/phase-1-output.json")));
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).get("account_id").asText(), actual.get(i).get("account_id").asText());
            assertEquals(expected.get(i).get("trade_count").asInt(), actual.get(i).get("trade_count").asInt());
            assertEquals(0, expected.get(i).get("proceeds").decimalValue()
                    .compareTo(actual.get(i).get("proceeds").decimalValue()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "T1,A1,,1.0,filled,2024-01-01",
            "T1,A1,2,abc,filled,2024-01-01",
            "T1,,2,1.0,filled,2024-01-01",
            "T1,A1,2,1.0,settled,2024-01-01",
            "T1,A1,2,1.0,filled,",
            "T1,A1,2,1.0,filled,2024-13-45",
            "T1,A1,2,1.0,filled"})
    void malformedRowsAreSkipped(String row) {
        TradeCsvParser.ParseResult r = TradeCsvParser.parse(List.of(HEADER, row));
        assertTrue(r.trades().isEmpty());
        assertEquals(1, r.skipped());
    }

    @Test
    void nonFilledAccountsOmittedAndTiesSortedById() {
        TradeCsvParser.ParseResult r = TradeCsvParser.parse(List.of(
                "T1,B,1,10,filled,2024-01-01", "T2,A,1,10,filled,2024-01-01",
                "T3,C,5,10,busted,2024-01-01", "T4,D,1,10,partially_filled,2024-01-01"));
        List<String> ids = Phase1.compute(r.trades()).stream()
                .map(Phase1.AccountProceeds::accountId).collect(Collectors.toList());
        assertEquals(List.of("A", "B"), ids);
    }
}
