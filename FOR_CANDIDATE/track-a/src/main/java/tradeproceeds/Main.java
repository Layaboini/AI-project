package tradeproceeds;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** CLI entry point. Run from the FOR_CANDIDATE directory so default paths resolve. */
public final class Main {
    private static final String USAGE =
            "usage:\n"
            + "  phase1 [--trades PATH] [--out PATH]\n"
            + "  phase2 [--trades PATH] [--accounts PATH] [--out PATH]\n"
            + "  top-accounts --start YYYY-MM-DD --end YYYY-MM-DD --limit N --offset N\n"
            + "               [--trades PATH] [--events DIR]";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length == 0) {
            System.err.println(USAGE);
            return 1;
        }
        Map<String, String> opts = options(args);
        Path trades = Path.of(opts.getOrDefault("trades", "data/trades.csv"));
        try {
            switch (args[0]) {
                case "phase1":
                    Phase1.run(trades, Path.of(opts.getOrDefault("out", "out/phase1.json")));
                    return 0;
                case "phase2":
                    Phase2.run(trades,
                               Path.of(opts.getOrDefault("accounts", "data/accounts.csv")),
                               Path.of(opts.getOrDefault("out", "out/phase2.csv")));
                    return 0;
                case "top-accounts":
                    System.out.println(Phase3.topAccounts(trades,
                            Path.of(opts.getOrDefault("events", "data/events")),
                            opts.getOrDefault("start", ""), opts.getOrDefault("end", ""),
                            intOption(opts, "limit"), intOption(opts, "offset")));
                    return 0;
                default:
                    System.err.println(USAGE);
                    return 1;
            }
        } catch (TopAccountsQuery.InvalidQueryException e) {
            System.err.println("error: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return 1;
        }
    }

    /** Missing or non-numeric values become Integer.MIN_VALUE, which validation then rejects. */
    private static int intOption(Map<String, String> opts, String key) {
        try {
            return Integer.parseInt(opts.getOrDefault(key, ""));
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    static Map<String, String> options(String[] args) {
        Map<String, String> o = new HashMap<>();
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) o.put(args[i].substring(2), args[++i]);
        }
        return o;
    }
}
