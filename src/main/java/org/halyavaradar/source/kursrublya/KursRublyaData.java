package org.halyavaradar.source.kursrublya;

import java.time.LocalDate;
import java.util.List;

public record KursRublyaData(
        String apiVersion,
        String currencyCode,
        String quoteCurrency,
        LocalDate from,
        LocalDate to,
        int count,
        List<KursRublyaPoint> points,
        String sourceUrl,
        String attribution,
        String licenseUrl,
        String siteUrl
) {
}
