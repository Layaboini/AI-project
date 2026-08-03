using TradeEngine.Models;

namespace TradeEngine.Parsing;

/// <summary>Parses the base trade blotter from CSV.</summary>
public static class TradeCsvParser
{
    private const string Header = "trade_id,account_id,quantity,price,status,trade_date";

    public static List<Trade> ParseFile(string path)
        => Parse(File.ReadAllLines(path));

    public static List<Trade> Parse(IReadOnlyList<string> lines)
    {
        var trades = new List<Trade>();
        for (var i = 0; i < lines.Count; i++)
        {
            var line = lines[i];
            if (i == 0 && line.StartsWith("trade_id")) continue;
            if (string.IsNullOrWhiteSpace(line)) continue;

            var cols = line.Split(',');
            if (cols.Length < 6) continue;
            if (!int.TryParse(cols[2], out var qty)) continue;
            if (!decimal.TryParse(cols[3], out var price)) continue;

            trades.Add(new Trade
            {
                TradeId = cols[0].Trim(),
                AccountId = cols[1].Trim(),
                Quantity = qty,
                Price = price,
                Status = cols[4].Trim(),
                TradeDate = cols[5].Trim(),
            });
        }
        return trades;
    }
}
