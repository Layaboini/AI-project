using TradeEngine.Domain;
using TradeEngine.Models;

namespace TradeEngine.Query;

public sealed record StatusResult(string Status, decimal NetValue, int TradeCount);

/// <summary>Reports net value grouped by trade status, after events are applied.</summary>
public sealed class ValueByStatusQuery
{
    private readonly IReadOnlyList<Trade> _baseTrades;
    private readonly IReadOnlyList<TradeEvent> _events;

    public ValueByStatusQuery(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
    {
        _baseTrades = baseTrades;
        _events = events;
    }

    /// <summary>
    /// Groups the counted trades (everything except cancelled, per
    /// <see cref="RevenueCalculator.Counts"/>) by status. Rows are ordered by status.
    /// </summary>
    public IReadOnlyList<StatusResult> Run()
    {
        var state = StateBuilder.Build(_baseTrades, _events);
        return Aggregate(state.Values);
    }

    internal static IReadOnlyList<StatusResult> Aggregate(IEnumerable<Trade> trades)
    {
        var agg = new Dictionary<string, (decimal Net, int Count)>(StringComparer.Ordinal);
        foreach (var t in trades)
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
}
