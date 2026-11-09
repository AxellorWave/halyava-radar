package org.halyavaradar.model;

import java.math.BigDecimal;

public class GameOffer {
    private final String source;
    private final String sourceId;
    private final String title;
    private final OfferType type;
    private final String url;
    private final BigDecimal price;
    private final String currency;

    public GameOffer(String source, String sourceId, String title, OfferType type, String url, BigDecimal price, String currency) {
        this.source = source;
        this.sourceId = sourceId;
        this.title = title;
        this.type = type;
        this.url = url;
        this.price = price;
        this.currency = currency;
    }

    public String getSource() {
        return source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title;
    }

    public OfferType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
