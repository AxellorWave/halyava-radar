package org.halyavaradar.source;

import org.halyavaradar.model.GameOffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SourceRegistryTest {
    @Test
    void shouldCreateEmptyRegistry() {
        SourceRegistry registry = new SourceRegistry(List.of());
        assertTrue(registry.getSourceNames().isEmpty());
    }

    @Test
    void shouldReturnRegisteredSource() {
        OfferSource cheapshark = createSource("cheapshark");
        OfferSource freetogame = createSource("freetogame");
        SourceRegistry registry = new SourceRegistry(List.of(cheapshark, freetogame));

        assertSame(cheapshark, registry.getSource("cheapshark"));
        assertSame(freetogame, registry.getSource("freetogame"));
    }

    @Test
    void shouldRejectUnknownSource() {
        OfferSource source = createSource("cheapshark");
        SourceRegistry registry = new SourceRegistry(List.of(source));

        assertThrows(IllegalArgumentException.class, () -> registry.getSource("unknown"));
        assertSame(source, registry.getSource("cheapshark"));
    }

    @Test
    void shouldRejectDuplicateSourceNames() {
        OfferSource first = createSource("cheapshark");
        OfferSource second = createSource("cheapshark");

        assertThrows(IllegalArgumentException.class,
                () -> new SourceRegistry(List.of(first, second)));
    }

    @Test
    void shouldReturnSourceNamesInRegistrationOrder() {
        SourceRegistry registry = new SourceRegistry(List.of(
                createSource("freetogame"),
                createSource("cheapshark"),
                createSource("isthereanydeal")
        ));

        assertIterableEquals(
                List.of("freetogame", "cheapshark", "isthereanydeal"),
                registry.getSourceNames()
        );
    }

    @Test
    void shouldReturnUnmodifiableSourceNames() {
        OfferSource source = createSource("cheapshark");
        SourceRegistry registry = new SourceRegistry(List.of(source));
        Set<String> names = registry.getSourceNames();

        assertThrows(UnsupportedOperationException.class, () -> names.add("freetogame"));
        assertThrows(UnsupportedOperationException.class, () -> names.remove("cheapshark"));
        assertThrows(UnsupportedOperationException.class, names::clear);
        assertEquals(Set.of("cheapshark"), registry.getSourceNames());
        assertSame(source, registry.getSource("cheapshark"));
    }

    private OfferSource createSource(String name) {
        return new OfferSource() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public List<GameOffer> fetchOffers() {
                return List.of();
            }
        };
    }
}
