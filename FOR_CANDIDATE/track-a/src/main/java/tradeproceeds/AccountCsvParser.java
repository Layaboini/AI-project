package tradeproceeds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parses accounts.csv into an account_id to region map. */
public final class AccountCsvParser {
    private AccountCsvParser() {}

    public static Map<String, String> parseFile(Path path) throws IOException {
        return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
    }

    /**
     * Malformed rows (fewer than 3 columns, or blank account_id/region) are skipped.
     * If an account appears twice, the first row wins.
     */
    public static Map<String, String> parse(List<String> lines) {
        Map<String, String> regions = new HashMap<>();
        boolean first = true;
        for (String raw : lines) {
            String line = raw.trim();
            if (first) {
                first = false;
                if (line.startsWith("account_id")) continue;
            }
            if (line.isEmpty()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 3) continue;
            String id = cols[0].trim();
            String region = cols[2].trim();
            if (id.isEmpty() || region.isEmpty()) continue;
            regions.putIfAbsent(id, region);
        }
        return regions;
    }
}
