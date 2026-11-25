using TradeEngine.Domain;
using TradeEngine.Models;

namespace TradeEngine.Query;

public sealed record AccountResult(string AccountId, decimal NetValue, int TradeCount);

/// <summary>Ranks accounts by net value within a date window.</summary>
public sealed class TopAccountsQuery
{
    private readonly TradeLedger _ledger;

    /// <summary>Replays the events once; every later <see cref="Run"/> reuses that state.</summary>
    public TopAccountsQuery(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
        : this(BuildLedger(baseTrades, events))
    {
    }

    /// <summary>Queries a shared, long-lived ledger (events may keep arriving between runs).</summary>
    public TopAccountsQuery(TradeLedger ledger) => _ledger = ledger;

    public IReadOnlyList<AccountResult> Run(string start, string end, int limit, int offset)
    {
        var agg = new Dictionary<string, (decimal Net, int Count)>();
        foreach (var t in _ledger.Trades.Values)
        {
            if (!RevenueCalculator.Counts(t)) continue;
            if (string.CompareOrdinal(t.TradeDate, start) < 0) continue;
            if (string.CompareOrdinal(t.TradeDate, end) > 0) continue;

            var cur = agg.TryGetValue(t.AccountId, out var v) ? v : (Net: 0m, Count: 0);
            agg[t.AccountId] = (cur.Net + RevenueCalculator.NetValue(t), cur.Count + 1);
        }

        return agg
            .Select(kv => new AccountResult(kv.Key, kv.Value.Net, kv.Value.Count))
            .OrderByDescending(r => r.NetValue)
            .ThenBy(r => r.AccountId, StringComparer.Ordinal)
            .Skip(offset)
            .Take(limit)
            .ToList();
    }

    private static TradeLedger BuildLedger(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
    {
        var ledger = new TradeLedger(baseTrades);
        ledger.Ingest(events);
        return ledger;
    }
}
