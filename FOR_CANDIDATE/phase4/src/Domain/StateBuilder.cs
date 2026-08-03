using TradeEngine.Models;

namespace TradeEngine.Domain;

/// <summary>Replays events over the base blotter to produce current state.</summary>
public static class StateBuilder
{
    public static Dictionary<string, Trade> Build(
        IEnumerable<Trade> baseTrades,
        IEnumerable<TradeEvent> events)
    {
        var state = new Dictionary<string, Trade>();
        foreach (var t in baseTrades)
            state[t.TradeId] = t.Clone();

        var ordered = events
            .OrderBy(e => e.Timestamp, StringComparer.Ordinal)
            .ThenBy(e => e.EventId, StringComparer.Ordinal);

        foreach (var e in ordered)
            Apply(state, e);

        return state;
    }

    private static void Apply(Dictionary<string, Trade> state, TradeEvent e)
    {
        switch (e.Type)
        {
            case "new":
                state[e.TradeId] = new Trade
                {
                    TradeId = e.TradeId,
                    AccountId = e.AccountId ?? "",
                    Quantity = e.Quantity ?? 0,
                    Price = e.Price ?? 0m,
                    Status = e.Status ?? "filled",
                    TradeDate = e.TradeDate ?? "",
                };
                break;

            case "fill":
                if (state.TryGetValue(e.TradeId, out var tf))
                    tf.Quantity += e.Quantity ?? 0;
                break;

            case "cancel":
                if (state.TryGetValue(e.TradeId, out var tc))
                    tc.Status = "cancelled";
                break;

            case "bust":
                if (state.TryGetValue(e.TradeId, out var tb))
                    tb.Status = "busted";
                break;
        }
    }
}
