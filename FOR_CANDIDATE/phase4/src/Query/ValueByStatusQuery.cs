using TradeEngine.Domain;
using TradeEngine.Models;

namespace TradeEngine.Query;

public sealed record StatusResult(string Status, decimal NetValue, int TradeCount);

/// <summary>Reports net value grouped by trade status, after events are applied.</summary>
public sealed class ValueByStatusQuery
{
    private readonly TradeLedger _ledger;

    public ValueByStatusQuery(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
        : this(BuildLedger(baseTrades, events))
    {
    }

    public ValueByStatusQuery(TradeLedger ledger) => _ledger = ledger;

    /// <summary>
    /// Groups the counted trades (everything except cancelled, per
    /// <see cref="RevenueCalculator.Counts"/>) by status. Rows are ordered by status.
    /// </summary>
    public IReadOnlyList<StatusResult> Run()
    {
        var agg = new Dictionary<string, (decimal Net, int Count)>(StringComparer.Ordinal);
        foreach (var t in _ledger.Trades.Values)
        {
            if (!RevenueCalculator.Counts(t)) continue;

            var cur = agg.TryGetValue(t.Status, out var v) ? v : (Net: 0m, Count: 0);
            agg[t.Status] = (cur.Net + RevenueCalculator.NetValue(t), cur.Count + 1);
        }

        return agg
            .Select(kv => new StatusResult(kv.Key, kv.Value.Net, kv.Value.Count))
            .OrderBy(r => r.Status, StringComparer.Ordinal)
            .ToList();
    }

    private static TradeLedger BuildLedger(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
    {
        var ledger = new TradeLedger(baseTrades);
        ledger.Ingest(events);
        return ledger;
    }
}
