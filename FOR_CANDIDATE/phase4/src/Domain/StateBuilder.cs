using TradeEngine.Models;

namespace TradeEngine.Domain;

/// <summary>Replays events over the base blotter to produce current state.</summary>
public static class StateBuilder
{
    /// <summary>
    /// One-shot replay. Delegates to <see cref="TradeLedger"/> so there is a single
    /// implementation of ordering and idempotency (each event_id applies once).
    /// </summary>
    public static Dictionary<string, Trade> Build(
        IEnumerable<Trade> baseTrades,
        IEnumerable<TradeEvent> events)
    {
        var ledger = new TradeLedger(baseTrades);
        ledger.Ingest(events);
        return new Dictionary<string, Trade>(ledger.Trades);
    }

    /// <summary>Applies a single event to the state in place.</summary>
    internal static void Apply(Dictionary<string, Trade> state, TradeEvent e)
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
