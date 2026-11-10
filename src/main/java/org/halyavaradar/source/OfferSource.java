package org.halyavaradar.source;

import org.halyavaradar.model.GameOffer;

import java.util.List;

public interface OfferSource {
    String getName();

    List<GameOffer> fetchOffers();
}
