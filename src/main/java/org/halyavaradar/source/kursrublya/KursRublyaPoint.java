package org.halyavaradar.source.kursrublya;

import java.math.BigDecimal;
import java.time.LocalDate;

public record KursRublyaPoint(LocalDate date, BigDecimal ratePerUnit) {
}
