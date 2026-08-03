using TradeEngine.Domain;
using TradeEngine.Models;

namespace TradeEngine.Query;

public sealed record AccountResult(string AccountId, decimal NetValue, int TradeCount);

/// <summary>Ranks accounts by net value within a date window.</summary>
public sealed class TopAccountsQuery
{
    private readonly IReadOnlyList<Trade> _baseTrades;
    private readonly IReadOnlyList<TradeEvent> _events;

    public TopAccountsQuery(IReadOnlyList<Trade> baseTrades, IReadOnlyList<TradeEvent> events)
    {
        _baseTrades = baseTrades;
        _events = events;
    }

    public IReadOnlyList<AccountResult> Run(string start, string end, int limit, int offset)
    {
        var state = StateBuilder.Build(_baseTrades, _events);

        var agg = new Dictionary<string, (decimal Net, int Count)>();
        foreach (var t in state.Values)
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
}
