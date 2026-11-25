using TradeEngine.Domain;
using TradeEngine.Models;
using TradeEngine.Query;
using Xunit;

namespace TradeEngine.Tests;

/// <summary>4c: state is replayed once and then reused or incrementally updated.</summary>
public class TradeLedgerTests
{
    private static Trade Base() => new()
    {
        TradeId = "T1", AccountId = "A001", Quantity = 1, Price = 50m, Status = "filled", TradeDate = "2024-05-01",
    };

    private static TradeEvent Fill(string id, string ts, int qty) => new()
    {
        EventId = id, Type = "fill", Timestamp = ts, TradeId = "T1", Quantity = qty,
    };

    [Fact]
    public void Repeated_Queries_Do_Not_Replay_Events()
    {
        var ledger = new TradeLedger(new[] { Base() });
        ledger.Ingest(new[] { Fill("F1", "2024-05-02T09:00:00Z", 2), Fill("F2", "2024-05-03T09:00:00Z", 1) });
        var query = new TopAccountsQuery(ledger);

        for (var i = 0; i < 5; i++) query.Run("2024-05-01", "2024-05-31", 10, 0);

        Assert.Equal(0, ledger.RebuildCount);
        Assert.Equal(4, ledger.Trades["T1"].Quantity);
    }

    [Fact]
    public void In_Order_Live_Events_Are_Applied_Incrementally_Without_Rebuild()
    {
        var ledger = new TradeLedger(new[] { Base() });
        var query = new TopAccountsQuery(ledger);
        Assert.Equal(50m, query.Run("2024-05-01", "2024-05-31", 10, 0)[0].NetValue);

        ledger.Ingest(Fill("F1", "2024-05-02T09:00:00Z", 2));

        Assert.Equal(150m, query.Run("2024-05-01", "2024-05-31", 10, 0)[0].NetValue);
        Assert.Equal(0, ledger.RebuildCount);
    }

    [Fact]
    public void Out_Of_Order_Event_Gives_Same_Result_As_Full_Replay()
    {
        var late = new TradeEvent { EventId = "B1", Type = "bust", Timestamp = "2024-05-05T09:00:00Z", TradeId = "T1" };
        var early = Fill("F1", "2024-05-02T09:00:00Z", 2);
        var ledger = new TradeLedger(new[] { Base() });
        ledger.Ingest(late);
        ledger.Ingest(early);

        var expected = StateBuilder.Build(new[] { Base() }, new[] { early, late });

        Assert.Equal(expected["T1"].Quantity, ledger.Trades["T1"].Quantity);
        Assert.Equal(expected["T1"].Status, ledger.Trades["T1"].Status);
        Assert.Equal("busted", ledger.Trades["T1"].Status);
        Assert.Equal(3, ledger.Trades["T1"].Quantity);
        Assert.Equal(1, ledger.RebuildCount);
    }

    [Fact]
    public void Redelivered_Event_Is_Ignored_On_The_Incremental_Path()
    {
        var ledger = new TradeLedger(new[] { Base() });
        var f = Fill("F1", "2024-05-02T09:00:00Z", 2);

        ledger.Ingest(f);
        ledger.Ingest(f);

        Assert.Equal(3, ledger.Trades["T1"].Quantity);
        Assert.Equal(0, ledger.RebuildCount);
    }
}
