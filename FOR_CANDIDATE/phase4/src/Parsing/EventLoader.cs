using System.Text.Json;
using TradeEngine.Models;

namespace TradeEngine.Parsing;

/// <summary>Loads events from every JSON file in a directory.</summary>
public static class EventLoader
{
    private static readonly JsonSerializerOptions Options = new()
    {
        PropertyNameCaseInsensitive = true,
    };

    public static List<TradeEvent> LoadDirectory(string dir)
    {
        var events = new List<TradeEvent>();
        foreach (var file in Directory.EnumerateFiles(dir, "*.json").OrderBy(f => f))
        {
            events.AddRange(LoadFile(file));
        }
        return events;
    }

    public static List<TradeEvent> LoadFile(string path)
    {
        var json = File.ReadAllText(path);
        return JsonSerializer.Deserialize<List<TradeEvent>>(json, Options) ?? new List<TradeEvent>();
    }
}
