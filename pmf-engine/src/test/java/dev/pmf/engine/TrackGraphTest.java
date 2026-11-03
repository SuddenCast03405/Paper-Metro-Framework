package dev.pmf.engine;

import dev.pmf.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackGraphTest {
    private TrackNode node(String id, String world, double x) {
        return new TrackNode(id, world, new Vec3(x, 64, 0), 12.0);
    }

    @Test void resolvesOnlyConnectedRoutesInOneWorld() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("A", "world", 0));
        graph.addNode(node("B", "world", 10));
        graph.addNode(node("C", "world", 20));
        assertTrue(graph.connect("A", "B"));
        assertTrue(graph.connect("B", "C"));
        assertEquals(List.of("A", "B", "C"),
                graph.route(List.of("A", "B", "C")).stream().map(TrackNode::id).toList());
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("A", "C")));
    }

    @Test void rejectsUnknownNodesAndCrossWorldRoutes() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("A", "world", 0));
        graph.addNode(node("B", "nether", 10));
        graph.connect("A", "B");
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("A", "missing")));
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("A", "B")));
    }

    @Test void removingNodeAlsoRemovesItsConnections() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("A", "world", 0));
        graph.addNode(node("B", "world", 10));
        graph.addNode(node("C", "world", 20));
        graph.connect("A", "B");
        graph.connect("B", "C");
        graph.removeNode("B");
        assertNull(graph.node("B"));
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("A", "B")));
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("B", "C")));
    }

    @Test void automaticallyFindsShortestConnectedRoute() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("A", "world", 0));
        graph.addNode(node("B", "world", 10));
        graph.addNode(node("C", "world", 20));
        graph.addNode(node("D", "world", 30));
        graph.connect("A", "B");
        graph.connect("B", "C");
        graph.connect("A", "D");
        graph.connect("D", "C");
        assertEquals(List.of("A", "B", "C"),
                graph.findRoute("A", "C").stream().map(TrackNode::id).toList());
        assertThrows(IllegalArgumentException.class, () -> graph.findRoute("A", "missing"));
    }

    @Test void shortestPathHonorsTheSelectedSwitchBranch() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("stem", "world", 0));
        graph.addNode(node("straight", "world", 10));
        graph.addNode(node("diverge", "world", 20));
        graph.connect("stem", "straight");
        graph.connect("stem", "diverge");
        graph.registerSwitch(new TrackSwitch("switch:west", "world",
                "stem", "straight", "diverge", SwitchRoute.STRAIGHT));

        assertEquals(List.of("stem", "straight"),
                graph.findRoute("stem", "straight").stream().map(TrackNode::id).toList());
        assertThrows(IllegalArgumentException.class, () -> graph.findRoute("stem", "diverge"));
        assertThrows(IllegalArgumentException.class, () -> graph.route(List.of("stem", "diverge")));

        graph.setSwitchRoute("switch:west", SwitchRoute.DIVERGING);
        assertEquals(List.of("stem", "diverge"),
                graph.findRoute("stem", "diverge").stream().map(TrackNode::id).toList());
        assertThrows(IllegalArgumentException.class, () -> graph.findRoute("stem", "straight"));
    }

    @Test void switchRegistrationValidatesTopologyAndNodeRemovalCleansItUp() {
        TrackGraph graph = new TrackGraph();
        graph.addNode(node("stem", "world", 0));
        graph.addNode(node("straight", "world", 10));
        graph.addNode(node("diverge", "world", 20));
        TrackSwitch turnout = new TrackSwitch("switch:1", "world",
                "stem", "straight", "diverge", SwitchRoute.STRAIGHT);
        assertThrows(IllegalArgumentException.class, () -> graph.registerSwitch(turnout));
        graph.connect("stem", "straight");
        graph.connect("stem", "diverge");
        graph.registerSwitch(turnout);
        assertThrows(IllegalArgumentException.class, () -> graph.registerSwitch(
                new TrackSwitch("switch:2", "world", "stem", "straight", "diverge", SwitchRoute.DIVERGING)));
        graph.removeNode("diverge");
        assertNull(graph.trackSwitch("switch:1"));
        assertTrue(graph.switches().isEmpty());
    }
}
