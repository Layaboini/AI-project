package tradeproceeds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** A trade lifecycle event as stored in data/events/*.json. Optional fields are null when absent. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TradeEvent {
    @JsonProperty("event_id") public String eventId;
    @JsonProperty("type") public String type;
    @JsonProperty("timestamp") public String timestamp;
    @JsonProperty("trade_id") public String tradeId;
    @JsonProperty("account_id") public String accountId;
    @JsonProperty("quantity") public Integer quantity;
    @JsonProperty("price") public BigDecimal price;
    @JsonProperty("status") public String status;
    @JsonProperty("trade_date") public String tradeDate;
}
