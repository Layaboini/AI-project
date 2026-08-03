using System.Globalization;
using System.Text.Json;
using TradeEngine.Parsing;
using TradeEngine.Query;

namespace TradeEngine;

public static class Program
{
    public static int Main(string[] args)
    {
        if (args.Length == 0 || args[0] != "top-accounts")
        {
            Console.Error.WriteLine(
                "usage: top-accounts --start YYYY-MM-DD --end YYYY-MM-DD --limit N --offset N [--data DIR]");
            return 1;
        }

        var opts = ParseOptions(args);
        var dataDir = opts.GetValueOrDefault("data", "data");
        var start = opts.GetValueOrDefault("start", "");
        var end = opts.GetValueOrDefault("end", "");

        if (!IsDate(start) || !IsDate(end))
        {
            Console.Error.WriteLine("error: --start and --end must be valid YYYY-MM-DD dates");
            return 1;
        }
        if (string.CompareOrdinal(start, end) > 0)
        {
            Console.Error.WriteLine("error: --start must not be after --end");
            return 1;
        }
        if (!TryPositive(opts, "limit", out var limit))
        {
            Console.Error.WriteLine("error: --limit must be a positive integer");
            return 1;
        }
        if (!TryNonNegative(opts, "offset", out var offset))
        {
            Console.Error.WriteLine("error: --offset must be a non-negative integer");
            return 1;
        }

        var tradesPath = Path.Combine(dataDir, "trades.csv");
        var eventsDir = Path.Combine(dataDir, "events");
        var baseTrades = TradeCsvParser.ParseFile(tradesPath);
        var events = Directory.Exists(eventsDir)
            ? EventLoader.LoadDirectory(eventsDir)
            : new();

        var query = new TopAccountsQuery(baseTrades, events);
        var results = query.Run(start, end, limit, offset);

        var payload = results.Select(r => new
        {
            account_id = r.AccountId,
            net_value = r.NetValue,
            trade_count = r.TradeCount,
        });
        Console.WriteLine(JsonSerializer.Serialize(payload, new JsonSerializerOptions { WriteIndented = true }));
        return 0;
    }

    private static Dictionary<string, string> ParseOptions(string[] args)
    {
        var opts = new Dictionary<string, string>();
        for (var i = 1; i < args.Length - 1; i++)
        {
            if (args[i].StartsWith("--"))
                opts[args[i][2..]] = args[i + 1];
        }
        return opts;
    }

    private static bool IsDate(string s) =>
        DateTime.TryParseExact(s, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);

    private static bool TryPositive(Dictionary<string, string> opts, string key, out int value)
        => int.TryParse(opts.GetValueOrDefault(key, ""), out value) && value > 0;

    private static bool TryNonNegative(Dictionary<string, string> opts, string key, out int value)
        => int.TryParse(opts.GetValueOrDefault(key, ""), out value) && value >= 0;
}
