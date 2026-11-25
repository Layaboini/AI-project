package tradeproceeds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Drives Main.run directly and captures stdout/stderr, so exit codes and messages are tested. */
class MainCliTest {
    private record Result(int exit, String out, String err) {}

    private static Result run(String... args) {
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
            int code = Main.run(args);
            return new Result(code, out.toString(), err.toString());
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
        }
    }

    private static Path writeTrades(Path dir) throws Exception {
        Path trades = dir.resolve("trades.csv");
        Files.writeString(trades, String.join("\n",
                "trade_id,account_id,quantity,price,status,trade_date",
                "T1,A001,2,50.00,filled,2024-05-01",
                "T2,A002,1,10.00,busted,2024-05-02",
                "bad row"));
        return trades;
    }

    @Test
    void noArgumentsPrintsUsageAndFails() {
        Result r = run();
        assertEquals(1, r.exit());
        assertTrue(r.err().contains("usage"));
    }

    @Test
    void unknownCommandFails() {
        Result r = run("bogus");
        assertEquals(1, r.exit());
        assertTrue(r.err().contains("usage"));
    }

    @Test
    void phase1WritesFileAndReportsSkippedRowsOnStderr(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("nested/out.json");
        Result r = run("phase1", "--trades", writeTrades(dir).toString(), "--out", out.toString());
        assertEquals(0, r.exit());
        assertEquals("1", r.err().trim());
        assertTrue(Files.readString(out).contains("\"A001\""));
    }

    @Test
    void missingInputFileIsAReportedErrorNotACrash(@TempDir Path dir) {
        Result r = run("phase1", "--trades", dir.resolve("nope.csv").toString());
        assertEquals(1, r.exit());
        assertTrue(r.err().startsWith("error:"));
    }

    @Test
    void topAccountsHappyPathPrintsJsonAndNoEventsDirIsFine(@TempDir Path dir) throws Exception {
        Result r = run("top-accounts", "--trades", writeTrades(dir).toString(),
                "--events", dir.resolve("none").toString(),
                "--start", "2024-05-01", "--end", "2024-05-31", "--limit", "5", "--offset", "0");
        assertEquals(0, r.exit());
        assertTrue(r.out().contains("\"account_id\" : \"A001\""));
        assertTrue(r.out().contains("100.00"));
    }

    @Test
    void invalidTopAccountsParametersGiveClearMessagesAndExitCodeOne(@TempDir Path dir) throws Exception {
        String trades = writeTrades(dir).toString();
        assertError(run("top-accounts", "--trades", trades, "--start", "2024-13-01", "--end", "2024-05-31",
                "--limit", "1", "--offset", "0"), "start must be a valid");
        assertError(run("top-accounts", "--trades", trades, "--start", "2024-06-01", "--end", "2024-05-31",
                "--limit", "1", "--offset", "0"), "start must not be after end");
        assertError(run("top-accounts", "--trades", trades, "--start", "2024-05-01", "--end", "2024-05-31",
                "--limit", "0", "--offset", "0"), "limit must be a positive integer");
        assertError(run("top-accounts", "--trades", trades, "--start", "2024-05-01", "--end", "2024-05-31",
                "--limit", "abc", "--offset", "0"), "limit must be a positive integer");
        assertError(run("top-accounts", "--trades", trades, "--start", "2024-05-01", "--end", "2024-05-31",
                "--limit", "1", "--offset", "-1"), "offset must be a non-negative integer");
        assertError(run("top-accounts", "--trades", trades, "--limit", "1", "--offset", "0"),
                "start must be a valid");
    }

    private static void assertError(Result r, String fragment) {
        assertEquals(1, r.exit());
        assertTrue(r.err().contains(fragment), "stderr was: " + r.err());
        assertEquals("", r.out());
    }
}
