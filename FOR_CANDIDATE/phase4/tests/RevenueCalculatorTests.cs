using TradeEngine.Domain;
using TradeEngine.Models;
using TradeEngine.Query;
using Xunit;

namespace TradeEngine.Tests;

public class RevenueCalculatorTests
{
    [Fact]
    public void Filled_Trade_Is_Positive()
    {
        var t = new Trade { Quantity = 3, Price = 50m, Status = "filled" };
        Assert.Equal(150m, RevenueCalculator.NetValue(t));
    }

    [Fact]
    public void Busted_Trade_Is_Negative()
    {
        var t = new Trade { Quantity = 3, Price = 30m, Status = "busted" };
        Assert.Equal(-90m, RevenueCalculator.NetValue(t));
    }

    [Fact]
    public void Cancelled_Trade_Does_Not_Count()
    {
        var t = new Trade { Quantity = 3, Price = 30m, Status = "cancelled" };
        Assert.False(RevenueCalculator.Counts(t));
        Assert.Equal(0m, RevenueCalculator.NetValue(t));
    }
}

public class StateBuilderTests
{
    [Fact]
    public void Fill_Adds_To_Quantity()
    {
        var baseTrades = new List<Trade>
        {
            new() { TradeId = "T1", AccountId = "A001", Quantity = 1, Price = 50m, Status = "filled", TradeDate = "2024-05-01" },
        };
        var events = new List<TradeEvent>
        {
            new() { EventId = "F1", Type = "fill", Timestamp = "2024-05-02T09:00:00Z", TradeId = "T1", Quantity = 2 },
        };

        var state = StateBuilder.Build(baseTrades, events);

        Assert.Equal(3, state["T1"].Quantity);
    }

    [Fact]
    public void Bust_Sets_Status()
    {
        var baseTrades = new List<Trade>
        {
            new() { TradeId = "T2", AccountId = "A002", Quantity = 3, Price = 30m, Status = "filled", TradeDate = "2024-05-01" },
        };
        var events = new List<TradeEvent>
        {
            new() { EventId = "B1", Type = "bust", Timestamp = "2024-05-04T09:00:00Z", TradeId = "T2" },
        };

        var state = StateBuilder.Build(baseTrades, events);

        Assert.Equal("busted", state["T2"].Status);
    }
}

public class TopAccountsQueryTests
{
    [Fact]
    public void Ranks_By_Net_Value_Descending()
    {
        var baseTrades = new List<Trade>
        {
            new() { TradeId = "T1", AccountId = "A001", Quantity = 3, Price = 50m, Status = "filled", TradeDate = "2024-05-01" },
            new() { TradeId = "T2", AccountId = "A002", Quantity = 3, Price = 30m, Status = "filled", TradeDate = "2024-05-01" },
        };

        var query = new TopAccountsQuery(baseTrades, new List<TradeEvent>());
        var results = query.Run("2024-05-01", "2024-05-31", 10, 0);

        Assert.Equal("A001", results[0].AccountId);
        Assert.Equal(150m, results[0].NetValue);
        Assert.Equal("A002", results[1].AccountId);
    }
}
