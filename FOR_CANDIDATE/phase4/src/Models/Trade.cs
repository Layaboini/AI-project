namespace TradeEngine.Models;

/// <summary>A trade and its current mutable state.</summary>
public sealed class Trade
{
    public string TradeId { get; set; } = "";
    public string AccountId { get; set; } = "";
    public int Quantity { get; set; }
    public decimal Price { get; set; }
    public string Status { get; set; } = "";
    public string TradeDate { get; set; } = "";

    public Trade Clone() => new()
    {
        TradeId = TradeId,
        AccountId = AccountId,
        Quantity = Quantity,
        Price = Price,
        Status = Status,
        TradeDate = TradeDate,
    };
}
