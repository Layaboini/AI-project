using TradeEngine.Models;

namespace TradeEngine.Domain;

/// <summary>Net-value rules by trade status.</summary>
public static class RevenueCalculator
{
    public static decimal NetValue(Trade t) => t.Status switch
    {
        "filled" or "partially_filled" => t.Quantity * t.Price,
        "busted" => -(t.Quantity * t.Price),
        _ => 0m,
    };

    /// <summary>Cancelled trades do not contribute to reports.</summary>
    public static bool Counts(Trade t) => t.Status != "cancelled";
}
