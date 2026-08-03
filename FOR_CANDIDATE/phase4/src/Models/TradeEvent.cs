using System.Text.Json.Serialization;

namespace TradeEngine.Models;

/// <summary>An event that changes a trade's state over time.</summary>
public sealed class TradeEvent
{
    [JsonPropertyName("event_id")] public string EventId { get; set; } = "";
    [JsonPropertyName("type")] public string Type { get; set; } = "";
    [JsonPropertyName("timestamp")] public string Timestamp { get; set; } = "";
    [JsonPropertyName("trade_id")] public string TradeId { get; set; } = "";
    [JsonPropertyName("account_id")] public string? AccountId { get; set; }
    [JsonPropertyName("quantity")] public int? Quantity { get; set; }
    [JsonPropertyName("price")] public decimal? Price { get; set; }
    [JsonPropertyName("status")] public string? Status { get; set; }
    [JsonPropertyName("trade_date")] public string? TradeDate { get; set; }
}
