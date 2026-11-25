using TradeEngine.Models;
using TradeEngine.Query;
using Xunit;

namespace TradeEngine.Tests;

public class ValueByStatusQueryTests
{
    private static Trade T(string id, int qty, decimal price, string status) => new()
    {
        TradeId = id, AccountId = "A001", Quantity = qty, Price = price, Status = status, TradeDate = "2024-05-01",
    };

    [Fact]
    public void Groups_By_Status_Using_Existing_Net_Value_Rules()
    {
        var trades = new List<Trade>
        {
            T("T1", 2, 10m, "filled"),
            T("T2", 1, 10m, "filled"),
            T("T3", 3, 5m, "partially_filled"),
            T("T4", 4, 5m, "busted"),
            T("T5", 9, 9m, "cancelled"),
        };

        var rows = new ValueByStatusQuery(trades, new List<TradeEvent>()).Run();

        Assert.Equal(new[] { "busted", "filled", "partially_filled" }, rows.Select(r => r.Status));
        Assert.Equal(-20m, rows[0].NetValue);
        Assert.Equal(30m, rows[1].NetValue);
        Assert.Equal(2, rows[1].TradeCount);
        Assert.Equal(15m, rows[2].NetValue);
    }

    [Fact]
    public void Reflects_Events_Applied_To_State()
    {
        var trades = new List<Trade> { T("T1", 2, 10m, "filled") };
        var events = new List<TradeEvent>
        {
            new() { EventId = "B1", Type = "bust", Timestamp = "2024-05-02T09:00:00Z", TradeId = "T1" },
        };

        var rows = new ValueByStatusQuery(trades, events).Run();

        var row = Assert.Single(rows);
        Assert.Equal("busted", row.Status);
        Assert.Equal(-20m, row.NetValue);
    }
}
