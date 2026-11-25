package tradeproceeds;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** CLI entry point. Run from the FOR_CANDIDATE directory so default paths resolve. */
public final class Main {
    private static final String USAGE = "usage: phase1 [--trades PATH] [--out PATH]";

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length == 0) {
            System.err.println(USAGE);
            return 1;
        }
        Map<String, String> opts = options(args);
        try {
            switch (args[0]) {
                case "phase1":
                    Phase1.run(Path.of(opts.getOrDefault("trades", "data/trades.csv")),
                               Path.of(opts.getOrDefault("out", "out/phase1.json")));
                    return 0;
                default:
                    System.err.println(USAGE);
                    return 1;
            }
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            return 1;
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
