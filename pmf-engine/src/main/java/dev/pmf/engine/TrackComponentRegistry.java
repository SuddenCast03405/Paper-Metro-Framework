package dev.pmf.engine;

import dev.pmf.api.TrackComponent;
import dev.pmf.api.TrackComponentType;
import java.util.*;

/** In-memory registry for PMF-owned rails, switches, signals and stations. */
public final class TrackComponentRegistry {
    private final Map<String, TrackComponent> components = new LinkedHashMap<>();

    public void register(TrackComponent component) {
        Objects.requireNonNull(component, "component");
        TrackComponent existing = components.get(component.id());
        if (existing != null && !existing.equals(component))
            throw new IllegalArgumentException("A different PMF component already uses id: " + component.id());
        components.put(component.id(), component);
    }

    public TrackComponent get(String id) { return components.get(id); }
    public TrackComponent remove(String id) { return components.remove(id); }
    public Collection<TrackComponent> all() { return Collections.unmodifiableCollection(components.values()); }
    public List<TrackComponent> byType(TrackComponentType type) {
        return components.values().stream().filter(c -> c.type() == type).toList();
    }
    public int size() { return components.size(); }
}
