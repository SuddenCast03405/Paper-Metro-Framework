package dev.pmf.api;

import java.util.Objects;

/**
 * Logical turnout connecting one shared stem to two mutually exclusive branches.
 * The backing Minecraft blocks remain ordinary vanilla rail blocks.
 */
public record TrackSwitch(
        String id,
        String world,
        String stemNodeId,
        String straightNodeId,
        String divergingNodeId,
        SwitchRoute selectedRoute
) {
    public TrackSwitch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(stemNodeId, "stemNodeId");
        Objects.requireNonNull(straightNodeId, "straightNodeId");
        Objects.requireNonNull(divergingNodeId, "divergingNodeId");
        Objects.requireNonNull(selectedRoute, "selectedRoute");
        if (id.isBlank() || world.isBlank()) {
            throw new IllegalArgumentException("Switch id and world cannot be blank");
        }
        if (stemNodeId.isBlank() || straightNodeId.isBlank() || divergingNodeId.isBlank()) {
            throw new IllegalArgumentException("Switch node IDs cannot be blank");
        }
        if (stemNodeId.equals(straightNodeId)
                || stemNodeId.equals(divergingNodeId)
                || straightNodeId.equals(divergingNodeId)) {
            throw new IllegalArgumentException("A switch requires three distinct track nodes");
        }
    }

    public String selectedBranchNodeId() {
        return selectedRoute == SwitchRoute.STRAIGHT ? straightNodeId : divergingNodeId;
    }

    /** A turnout only permits the stem-to-selected-branch connection. */
    public boolean allowsConnection(String firstNodeId, String secondNodeId) {
        return (stemNodeId.equals(firstNodeId) && selectedBranchNodeId().equals(secondNodeId))
                || (stemNodeId.equals(secondNodeId) && selectedBranchNodeId().equals(firstNodeId));
    }

    public TrackSwitch withRoute(SwitchRoute route) {
        return new TrackSwitch(id, world, stemNodeId, straightNodeId, divergingNodeId, route);
    }
}
