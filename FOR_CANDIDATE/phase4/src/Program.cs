using System.Globalization;
using System.Text.Json;
using TradeEngine.Parsing;
using TradeEngine.Query;

namespace TradeEngine;

public static class Program
{
    private const string Usage =
        "usage:\n" +
        "  top-accounts --start YYYY-MM-DD --end YYYY-MM-DD --limit N --offset N [--data DIR]\n" +
        "  value-by-status [--data DIR]";

    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public static int Main(string[] args)
    {
        if (args.Length == 0)
        {
            Console.Error.WriteLine(Usage);
            return 1;
        }

        var opts = ParseOptions(args);
        return args[0] switch
        {
            "top-accounts" => RunTopAccounts(opts),
            "value-by-status" => RunValueByStatus(opts),
            _ => Fail(Usage),
        };
    }

    private static int RunTopAccounts(Dictionary<string, string> opts)
    {
        var start = opts.GetValueOrDefault("start", "");
        var end = opts.GetValueOrDefault("end", "");

        if (!IsDate(start) || !IsDate(end))
            return Fail("error: --start and --end must be valid YYYY-MM-DD dates");
        if (string.CompareOrdinal(start, end) > 0)
            return Fail("error: --start must not be after --end");
        if (!TryPositive(opts, "limit", out var limit))
            return Fail("error: --limit must be a positive integer");
        if (!TryNonNegative(opts, "offset", out var offset))
            return Fail("error: --offset must be a non-negative integer");

        var (baseTrades, events) = Load(opts);
        var results = new TopAccountsQuery(baseTrades, events).Run(start, end, limit, offset);

        Print(results.Select(r => new
        {
            account_id = r.AccountId,
            net_value = r.NetValue,
            trade_count = r.TradeCount,
        }));
        return 0;
    }

    private static int RunValueByStatus(Dictionary<string, string> opts)
    {
        var (baseTrades, events) = Load(opts);
        var results = new ValueByStatusQuery(baseTrades, events).Run();

        Print(results.Select(r => new
        {
            status = r.Status,
            net_value = r.NetValue,
            trade_count = r.TradeCount,
        }));
        return 0;
    }

    private static (List<Models.Trade> BaseTrades, List<Models.TradeEvent> Events) Load(
        Dictionary<string, string> opts)
    {
        var dataDir = opts.GetValueOrDefault("data", "data");
        var baseTrades = TradeCsvParser.ParseFile(Path.Combine(dataDir, "trades.csv"));
        var eventsDir = Path.Combine(dataDir, "events");
        var events = Directory.Exists(eventsDir)
            ? EventLoader.LoadDirectory(eventsDir)
            : new();
        return (baseTrades, events);
    }

    private static void Print<T>(IEnumerable<T> rows) =>
        Console.WriteLine(JsonSerializer.Serialize(rows, JsonOptions));

    private static int Fail(string message)
    {
        Console.Error.WriteLine(message);
        return 1;
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
