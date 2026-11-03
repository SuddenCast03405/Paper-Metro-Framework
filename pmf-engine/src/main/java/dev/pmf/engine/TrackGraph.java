package dev.pmf.engine;

import dev.pmf.api.SwitchRoute;
import dev.pmf.api.TrackNode;
import dev.pmf.api.TrackSwitch;

import java.util.*;

public final class TrackGraph {
    private final Map<String, TrackNode> nodes = new LinkedHashMap<>();
    private final Map<String, Set<String>> edges = new HashMap<>();
    private final Map<String, TrackSwitch> switches = new LinkedHashMap<>();
    private final Map<String, String> switchByStem = new HashMap<>();

    public void addNode(TrackNode node) {
        nodes.put(node.id(), node);
        edges.computeIfAbsent(node.id(), ignored -> new LinkedHashSet<>());
    }

    public boolean connect(String first, String second) {
        if (!nodes.containsKey(first) || !nodes.containsKey(second)) return false;
        edges.computeIfAbsent(first, ignored -> new LinkedHashSet<>()).add(second);
        edges.computeIfAbsent(second, ignored -> new LinkedHashSet<>()).add(first);
        return true;
    }

    public void removeNode(String id) {
        nodes.remove(id);
        edges.remove(id);
        for (Set<String> adjacent : edges.values()) adjacent.remove(id);
        List<String> invalidSwitches = switches.values().stream()
                .filter(turnout -> turnout.stemNodeId().equals(id)
                        || turnout.straightNodeId().equals(id)
                        || turnout.divergingNodeId().equals(id))
                .map(TrackSwitch::id).toList();
        invalidSwitches.forEach(this::removeSwitch);
    }

    public TrackNode node(String id) {
        return nodes.get(id);
    }

    public Collection<TrackNode> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /** Registers a turnout over three already-connected nodes. */
    public void registerSwitch(TrackSwitch turnout) {
        Objects.requireNonNull(turnout, "turnout");
        TrackNode stem = nodes.get(turnout.stemNodeId());
        TrackNode straight = nodes.get(turnout.straightNodeId());
        TrackNode diverging = nodes.get(turnout.divergingNodeId());
        if (stem == null || straight == null || diverging == null) {
            throw new IllegalArgumentException("All switch track nodes must exist before registration");
        }
        if (!stem.world().equals(turnout.world())
                || !straight.world().equals(turnout.world())
                || !diverging.world().equals(turnout.world())) {
            throw new IllegalArgumentException("A switch cannot span worlds");
        }
        if (!edges.getOrDefault(turnout.stemNodeId(), Set.of()).contains(turnout.straightNodeId())
                || !edges.getOrDefault(turnout.stemNodeId(), Set.of()).contains(turnout.divergingNodeId())) {
            throw new IllegalArgumentException("Both switch branches must be connected to the stem node");
        }
        String existingAtStem = switchByStem.get(turnout.stemNodeId());
        if (existingAtStem != null && !existingAtStem.equals(turnout.id())) {
            throw new IllegalArgumentException("A track node can belong to only one switch stem");
        }
        TrackSwitch existing = switches.get(turnout.id());
        if (existing != null && !existing.equals(turnout)) {
            throw new IllegalArgumentException("Conflicting switch ID: " + turnout.id());
        }
        switches.put(turnout.id(), turnout);
        switchByStem.put(turnout.stemNodeId(), turnout.id());
    }

    public TrackSwitch trackSwitch(String id) {
        return switches.get(id);
    }

    public Collection<TrackSwitch> switches() {
        return Collections.unmodifiableCollection(switches.values());
    }

    public TrackSwitch setSwitchRoute(String id, SwitchRoute route) {
        TrackSwitch current = switches.get(id);
        if (current == null) throw new IllegalArgumentException("Unknown switch: " + id);
        TrackSwitch updated = current.withRoute(Objects.requireNonNull(route, "route"));
        switches.put(id, updated);
        return updated;
    }

    public TrackSwitch removeSwitch(String id) {
        TrackSwitch removed = switches.remove(id);
        if (removed != null) switchByStem.remove(removed.stemNodeId(), id);
        return removed;
    }

    private boolean connectionEnabled(String first, String second) {
        String switchId = switchByStem.get(first);
        if (switchId != null && !switches.get(switchId).allowsConnection(first, second)) return false;
        switchId = switchByStem.get(second);
        return switchId == null || switches.get(switchId).allowsConnection(first, second);
    }

    /** Finds the shortest currently permitted route using breadth-first search. */
    public List<TrackNode> findRoute(String start, String destination) {
        if (!nodes.containsKey(start)) throw new IllegalArgumentException("Unknown start node: " + start);
        if (!nodes.containsKey(destination)) throw new IllegalArgumentException("Unknown destination node: " + destination);
        Map<String, String> previous = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && !visited.contains(destination)) {
            String current = queue.removeFirst();
            for (String next : edges.getOrDefault(current, Set.of())) {
                if (connectionEnabled(current, next) && visited.add(next)) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
            }
        }
        if (!visited.contains(destination)) {
            throw new IllegalArgumentException("No connected rail route between " + start + " and " + destination
                    + " with current switch positions");
        }
        LinkedList<TrackNode> path = new LinkedList<>();
        String cursor = destination;
        while (cursor != null) {
            path.addFirst(nodes.get(cursor));
            cursor = previous.get(cursor);
        }
        String world = path.getFirst().world();
        if (path.stream().anyMatch(node -> !node.world().equals(world))) {
            throw new IllegalArgumentException("A route cannot cross worlds.");
        }
        return List.copyOf(path);
    }

    public List<TrackNode> route(List<String> ids) {
        if (ids.size() < 2) throw new IllegalArgumentException("A route needs at least two nodes.");
        List<TrackNode> result = new ArrayList<>();
        for (String id : ids) {
            TrackNode node = nodes.get(id);
            if (node == null) throw new IllegalArgumentException("Unknown track node: " + id);
            result.add(node);
        }
        for (int i = 0; i < ids.size() - 1; i++) {
            String from = ids.get(i), to = ids.get(i + 1);
            if (!edges.getOrDefault(from, Set.of()).contains(to)) {
                throw new IllegalArgumentException("Nodes are not connected: " + from + " -> " + to);
            }
            if (!connectionEnabled(from, to)) {
                throw new IllegalArgumentException("Route conflicts with current switch position: " + from + " -> " + to);
            }
        }
        String world = result.get(0).world();
        if (result.stream().anyMatch(node -> !node.world().equals(world))) {
            throw new IllegalArgumentException("A route cannot cross worlds.");
        }
        return List.copyOf(result);
    }
}
