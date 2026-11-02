package dev.pmf.api;

import java.util.Objects;

/** A PMF-owned infrastructure component independent of its backing world block. */
public record TrackComponent(String id, TrackComponentType type, String world, Vec3 position) {
    public TrackComponent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(position, "position");
        if (id.isBlank()) throw new IllegalArgumentException("Component id cannot be blank");
        if (world.isBlank()) throw new IllegalArgumentException("Component world cannot be blank");
    }
}
