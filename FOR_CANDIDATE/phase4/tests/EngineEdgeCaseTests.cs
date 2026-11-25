using TradeEngine.Domain;
using TradeEngine.Models;
using TradeEngine.Parsing;
using TradeEngine.Query;
using Xunit;

namespace TradeEngine.Tests;

public class EngineEdgeCaseTests
{
    private static Trade Base(string id = "T1", int qty = 1) => new()
    {
        TradeId = id, AccountId = "A001", Quantity = qty, Price = 50m, Status = "filled", TradeDate = "2024-05-01",
    };

    private static TradeEvent E(string id, string type, string ts, string tradeId, int? qty = null) => new()
    {
        EventId = id, Type = type, Timestamp = ts, TradeId = tradeId, Quantity = qty,
    };

    [Fact]
    public void Events_For_Unknown_Trades_Are_Ignored()
    {
        var events = new[]
        {
            E("E1", "fill", "2024-05-02T09:00:00Z", "NOPE", 5),
            E("E2", "cancel", "2024-05-02T10:00:00Z", "NOPE"),
            E("E3", "bust", "2024-05-02T11:00:00Z", "NOPE"),
        };

        var state = StateBuilder.Build(new[] { Base() }, events);

        Assert.Single(state);
        Assert.Equal(1, state["T1"].Quantity);
    }

    [Fact]
    public void Same_Timestamp_Ties_Break_By_Event_Id_Regardless_Of_Input_Order()
    {
        var bust = E("B", "bust", "2024-05-02T09:00:00Z", "T1");
        var fill = E("A", "fill", "2024-05-02T09:00:00Z", "T1", 2);

        var forward = StateBuilder.Build(new[] { Base() }, new[] { bust, fill });
        var reverse = StateBuilder.Build(new[] { Base() }, new[] { fill, bust });

        Assert.Equal(3, forward["T1"].Quantity);
        Assert.Equal("busted", forward["T1"].Status);
        Assert.Equal(forward["T1"].Quantity, reverse["T1"].Quantity);
        Assert.Equal(forward["T1"].Status, reverse["T1"].Status);
    }

    [Fact]
    public void New_Event_Replaces_An_Existing_Trade_And_Later_Fill_Adds_To_It()
    {
        var replace = new TradeEvent
        {
            EventId = "N1", Type = "new", Timestamp = "2024-05-02T09:00:00Z", TradeId = "T1",
            AccountId = "A009", Quantity = 4, Price = 10m, Status = "filled", TradeDate = "2024-05-03",
        };
        var fill = E("F1", "fill", "2024-05-03T09:00:00Z", "T1", 1);

        var state = StateBuilder.Build(new[] { Base() }, new[] { fill, replace });

        Assert.Equal("A009", state["T1"].AccountId);
        Assert.Equal(5, state["T1"].Quantity);
    }

    [Fact]
    public void Build_Does_Not_Mutate_The_Base_Trades()
    {
        var baseTrade = Base();

        StateBuilder.Build(new[] { baseTrade }, new[] { E("B1", "bust", "2024-05-02T09:00:00Z", "T1") });

        Assert.Equal("filled", baseTrade.Status);
    }

    [Fact]
    public void Earlier_Redelivery_Of_A_Known_Event_Id_Wins_And_Matches_Full_Replay()
    {
        var later = E("F1", "fill", "2024-05-05T09:00:00Z", "T1", 1);
        var earlier = E("F1", "fill", "2024-05-03T09:00:00Z", "T1", 9);
        var ledger = new TradeLedger(new[] { Base() });
        ledger.Ingest(later);
        ledger.Ingest(earlier);

        var expected = StateBuilder.Build(new[] { Base() }, new[] { later, earlier });

        Assert.Equal(expected["T1"].Quantity, ledger.Trades["T1"].Quantity);
        Assert.Equal(10, ledger.Trades["T1"].Quantity);
    }

    [Fact]
    public void Ledger_Matches_Full_Replay_For_Shuffled_Input()
    {
        var events = Enumerable.Range(1, 40)
            .Select(i => E($"F{i:D2}", "fill", $"2024-05-{(i % 28) + 1:D2}T09:00:00Z", "T1", i))
            .ToList();
        var shuffled = events.OrderBy(e => (e.Quantity * 7) % 13).ThenBy(e => e.EventId).ToList();
        var ledger = new TradeLedger(new[] { Base() });
        ledger.Ingest(shuffled);

        var expected = StateBuilder.Build(new[] { Base() }, events);

        Assert.Equal(expected["T1"].Quantity, ledger.Trades["T1"].Quantity);
    }

    [Fact]
    public void Window_Bounds_Are_Inclusive_And_Empty_Input_Is_Empty()
    {
        var trades = new List<Trade>
        {
            new() { TradeId = "T1", AccountId = "A", Quantity = 1, Price = 10m, Status = "filled", TradeDate = "2024-05-01" },
            new() { TradeId = "T2", AccountId = "B", Quantity = 1, Price = 20m, Status = "filled", TradeDate = "2024-05-31" },
            new() { TradeId = "T3", AccountId = "C", Quantity = 1, Price = 30m, Status = "filled", TradeDate = "2024-06-01" },
        };
        var query = new TopAccountsQuery(trades, new List<TradeEvent>());

        var rows = query.Run("2024-05-01", "2024-05-31", 10, 0);

        Assert.Equal(new[] { "B", "A" }, rows.Select(r => r.AccountId));
        Assert.Empty(query.Run("2024-05-01", "2024-05-31", 10, 5));
        Assert.Empty(new TopAccountsQuery(new List<Trade>(), new List<TradeEvent>()).Run("2024-01-01", "2024-12-31", 5, 0));
    }

    [Fact]
    public void Value_By_Status_Is_Empty_When_Everything_Is_Cancelled()
    {
        var trades = new List<Trade>
        {
            new() { TradeId = "T1", AccountId = "A", Quantity = 1, Price = 10m, Status = "cancelled", TradeDate = "2024-05-01" },
        };

        Assert.Empty(new ValueByStatusQuery(trades, new List<TradeEvent>()).Run());
    }

    [Fact]
    public void Csv_Parser_Skips_Short_Bad_Number_Rows_And_Blank_Lines()
    {
        var lines = new[]
        {
            "trade_id,account_id,quantity,price,status,trade_date",
            "",
            "T1,A,1,10.5,filled,2024-05-01",
            "T2,A,x,10.5,filled,2024-05-01",
            "T3,A,1",
            "T4,A,1,abc,filled,2024-05-01",
        };

        var trades = TradeCsvParser.Parse(lines);

        Assert.Equal("T1", Assert.Single(trades).TradeId);
    }
}
