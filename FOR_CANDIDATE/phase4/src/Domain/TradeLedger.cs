using TradeEngine.Models;

namespace TradeEngine.Domain;

/// <summary>
/// Long-lived current state: the base blotter plus every event ingested so far.
/// Replay work is paid once, not once per query:
/// <list type="bullet">
/// <item>an event at or after the last applied one (the normal live case) is applied in O(1);</item>
/// <item>an out-of-order event, or an earlier redelivery of a known event_id, only marks the
/// state stale; one O(E log E) rebuild happens lazily on the next read;</item>
/// <item>reads of unchanged state are O(1) (they return the cached dictionary).</item>
/// </list>
/// Results are identical to replaying everything from scratch in (timestamp, event_id) order,
/// applying each event_id once (its earliest occurrence).
/// </summary>
public sealed class TradeLedger
{
    private readonly List<Trade> _baseTrades;
    private readonly Dictionary<string, TradeEvent> _eventsById = new(StringComparer.Ordinal);
    private Dictionary<string, Trade> _state;
    private TradeEvent? _lastApplied;
    private bool _stale;

    public TradeLedger(IEnumerable<Trade> baseTrades)
    {
        _baseTrades = baseTrades.Select(t => t.Clone()).ToList();
        _state = CloneBase();
    }

    /// <summary>How many full replays have happened. Exposed so tests can assert on scaling behaviour.</summary>
    public int RebuildCount { get; private set; }

    /// <summary>Current trade state, replayed lazily if out-of-order input made it stale.</summary>
    public IReadOnlyDictionary<string, Trade> Trades
    {
        get
        {
            if (_stale) Rebuild();
            return _state;
        }
    }

    public void Ingest(IEnumerable<TradeEvent> events)
    {
        foreach (var e in events) Ingest(e);
    }

    public void Ingest(TradeEvent e)
    {
        if (_eventsById.TryGetValue(e.EventId, out var known))
        {
            // Redelivery. Keep the earliest occurrence in replay order, like a full replay would.
            if (Compare(e, known) < 0)
            {
                _eventsById[e.EventId] = e;
                _stale = true;
            }
            return;
        }

        _eventsById[e.EventId] = e;
        if (!_stale && (_lastApplied is null || Compare(e, _lastApplied) >= 0))
        {
            StateBuilder.Apply(_state, e);
            _lastApplied = e;
        }
        else
        {
            _stale = true;
        }
    }

    private void Rebuild()
    {
        RebuildCount++;
        _state = CloneBase();
        _lastApplied = null;
        foreach (var e in _eventsById.Values.OrderBy(x => x, EventOrder.Instance))
        {
            StateBuilder.Apply(_state, e);
            _lastApplied = e;
        }
        _stale = false;
    }

    private Dictionary<string, Trade> CloneBase()
    {
        var state = new Dictionary<string, Trade>();
        foreach (var t in _baseTrades) state[t.TradeId] = t.Clone();
        return state;
    }

    private static int Compare(TradeEvent a, TradeEvent b) => EventOrder.Instance.Compare(a, b);

    private sealed class EventOrder : IComparer<TradeEvent>
    {
        public static readonly EventOrder Instance = new();

        public int Compare(TradeEvent? a, TradeEvent? b)
        {
            var c = string.CompareOrdinal(a!.Timestamp, b!.Timestamp);
            return c != 0 ? c : string.CompareOrdinal(a.EventId, b.EventId);
        }
    }
}
