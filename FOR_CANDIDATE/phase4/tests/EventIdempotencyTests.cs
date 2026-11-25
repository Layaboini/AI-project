using TradeEngine.Domain;
using TradeEngine.Models;
using TradeEngine.Parsing;
using TradeEngine.Query;
using Xunit;

namespace TradeEngine.Tests;

/// <summary>Regression tests for 4a: a redelivered event must be applied exactly once.</summary>
public class EventIdempotencyTests
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
    public void Duplicate_Event_Id_Is_Applied_Once()
    {
        var events = new List<TradeEvent>
        {
            Fill("F1", "2024-05-02T09:00:00Z", 2),
            Fill("F1", "2024-05-02T09:00:00Z", 2),
        };

        var state = StateBuilder.Build(new[] { Base() }, events);

        Assert.Equal(3, state["T1"].Quantity);
    }

    [Fact]
    public void Distinct_Events_With_Same_Timestamp_Are_Both_Applied()
    {
        var events = new List<TradeEvent>
        {
            Fill("F1", "2024-05-02T09:00:00Z", 2),
            Fill("F2", "2024-05-02T09:00:00Z", 2),
        };

        var state = StateBuilder.Build(new[] { Base() }, events);

        Assert.Equal(5, state["T1"].Quantity);
    }

    [Fact]
    public void Event_Duplicated_Across_Batch_Files_Counts_Once()
    {
        var dir = Path.Combine(Path.GetTempPath(), "tradeengine-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(dir);
        try
        {
            const string json = "[{\"event_id\":\"F1\",\"type\":\"fill\",\"timestamp\":\"2024-05-02T09:00:00Z\",\"trade_id\":\"T1\",\"quantity\":2}]";
            File.WriteAllText(Path.Combine(dir, "batch-a.json"), json);
            File.WriteAllText(Path.Combine(dir, "batch-b.json"), json);

            var events = EventLoader.LoadDirectory(dir);
            var result = new TopAccountsQuery(new[] { Base() }, events).Run("2024-05-01", "2024-05-31", 10, 0);

            Assert.Equal(150m, Assert.Single(result).NetValue);
        }
        finally
        {
            Directory.Delete(dir, true);
        }
    }
}
