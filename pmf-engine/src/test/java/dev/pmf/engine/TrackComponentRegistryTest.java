package dev.pmf.engine;

import dev.pmf.api.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrackComponentRegistryTest {
    private TrackComponent component(String id, TrackComponentType type) {
        return new TrackComponent(id, type, "world", new Vec3(1, 64, 2));
    }

    @Test void registersAndQueriesDistinctComponentTypes() {
        TrackComponentRegistry registry = new TrackComponentRegistry();
        registry.register(component("pmf:rail:a", TrackComponentType.RAIL));
        registry.register(component("pmf:signal:a", TrackComponentType.SIGNAL));
        registry.register(component("pmf:station:a", TrackComponentType.STATION));
        assertEquals(3, registry.size());
        assertEquals(1, registry.byType(TrackComponentType.RAIL).size());
        assertEquals(1, registry.byType(TrackComponentType.SIGNAL).size());
        assertEquals(1, registry.byType(TrackComponentType.STATION).size());
    }

    @Test void permitsIdempotentRegistrationAndRejectsConflictingIds() {
        TrackComponentRegistry registry = new TrackComponentRegistry();
        TrackComponent rail = component("pmf:rail:a", TrackComponentType.RAIL);
        registry.register(rail); registry.register(rail);
        assertEquals(1, registry.size());
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(component("pmf:rail:a", TrackComponentType.SWITCH)));
    }

    @Test void removesOneComponentWithoutAffectingOtherTypes() {
        TrackComponentRegistry registry = new TrackComponentRegistry();
        registry.register(component("pmf:rail:a", TrackComponentType.RAIL));
        registry.register(component("pmf:signal:a", TrackComponentType.SIGNAL));
        assertEquals(TrackComponentType.RAIL, registry.remove("pmf:rail:a").type());
        assertNull(registry.get("pmf:rail:a"));
        assertEquals(1, registry.byType(TrackComponentType.SIGNAL).size());
    }
}
