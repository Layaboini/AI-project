using Xunit;

namespace TradeEngine.Tests;

/// <summary>
/// Drives Program.Main with redirected console streams. Console redirection is process-wide,
/// so these tests share a collection and never run in parallel with each other.
/// </summary>
[Collection("console")]
public class ProgramCliTests : IDisposable
{
    private readonly string _dir = Path.Combine(Path.GetTempPath(), "tradeengine-cli-" + Guid.NewGuid().ToString("N"));

    public ProgramCliTests()
    {
        Directory.CreateDirectory(Path.Combine(_dir, "events"));
        File.WriteAllText(Path.Combine(_dir, "trades.csv"), string.Join("\n",
            "trade_id,account_id,quantity,price,status,trade_date",
            "T1,A001,2,50.00,filled,2024-05-01",
            "T2,A002,1,10.00,busted,2024-05-02",
            "T3,A003,5,10.00,cancelled,2024-05-03"));
        File.WriteAllText(Path.Combine(_dir, "events", "e.json"),
            "[{\"event_id\":\"F1\",\"type\":\"fill\",\"timestamp\":\"2024-05-02T09:00:00Z\",\"trade_id\":\"T1\",\"quantity\":1}]");
    }

    public void Dispose() => Directory.Delete(_dir, true);

    private static (int Exit, string Out, string Err) Run(params string[] args)
    {
        var oldOut = Console.Out;
        var oldErr = Console.Error;
        var o = new StringWriter();
        var e = new StringWriter();
        try
        {
            Console.SetOut(o);
            Console.SetError(e);
            var code = Program.Main(args);
            return (code, o.ToString(), e.ToString());
        }
        finally
        {
            Console.SetOut(oldOut);
            Console.SetError(oldErr);
        }
    }

    [Fact]
    public void No_Args_And_Unknown_Command_Print_Usage_And_Fail()
    {
        var none = Run();
        var bogus = Run("bogus");

        Assert.Equal(1, none.Exit);
        Assert.Contains("usage", none.Err);
        Assert.Equal(1, bogus.Exit);
        Assert.Contains("value-by-status", bogus.Err);
    }

    [Fact]
    public void Value_By_Status_Prints_Grouped_Json_After_Events()
    {
        var r = Run("value-by-status", "--data", _dir);

        Assert.Equal(0, r.Exit);
        Assert.Contains("\"status\": \"filled\"", r.Out);
        Assert.Contains("150", r.Out); // 3 shares after the fill event, at 50
        Assert.Contains("\"status\": \"busted\"", r.Out);
        Assert.DoesNotContain("cancelled", r.Out);
    }

    [Fact]
    public void Top_Accounts_Happy_Path_Uses_The_Data_Dir()
    {
        var r = Run("top-accounts", "--start", "2024-05-01", "--end", "2024-05-31",
            "--limit", "5", "--offset", "0", "--data", _dir);

        Assert.Equal(0, r.Exit);
        Assert.Contains("\"account_id\": \"A001\"", r.Out);
        Assert.DoesNotContain("A003", r.Out);
    }

    [Theory]
    [InlineData("must be valid YYYY-MM-DD", "--start", "2024-13-01", "--end", "2024-05-31", "--limit", "1", "--offset", "0")]
    [InlineData("must not be after", "--start", "2024-06-01", "--end", "2024-05-31", "--limit", "1", "--offset", "0")]
    [InlineData("--limit must be a positive", "--start", "2024-05-01", "--end", "2024-05-31", "--limit", "0", "--offset", "0")]
    [InlineData("--limit must be a positive", "--start", "2024-05-01", "--end", "2024-05-31", "--limit", "abc", "--offset", "0")]
    [InlineData("--offset must be a non-negative", "--start", "2024-05-01", "--end", "2024-05-31", "--limit", "1", "--offset", "-1")]
    public void Invalid_Top_Accounts_Arguments_Fail_With_A_Clear_Message(string fragment, params string[] args)
    {
        var r = Run(new[] { "top-accounts" }.Concat(args).Concat(new[] { "--data", _dir }).ToArray());

        Assert.Equal(1, r.Exit);
        Assert.Contains(fragment, r.Err);
        Assert.Equal("", r.Out);
    }
}
