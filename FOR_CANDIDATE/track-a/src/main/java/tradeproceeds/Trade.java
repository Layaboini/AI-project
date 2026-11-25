package tradeproceeds;

import java.math.BigDecimal;

/** A trade and its current state. Mutable so events can amend it. */
public final class Trade {
    public String tradeId = "";
    public String accountId = "";
    public int quantity;
    public BigDecimal price = BigDecimal.ZERO;
    public String status = "";
    public String tradeDate = "";

    public Trade copy() {
        Trade t = new Trade();
        t.tradeId = tradeId;
        t.accountId = accountId;
        t.quantity = quantity;
        t.price = price;
        t.status = status;
        t.tradeDate = tradeDate;
        return t;
    }
}
