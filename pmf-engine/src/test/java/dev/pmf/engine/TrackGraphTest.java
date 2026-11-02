package dev.pmf.engine;
import dev.pmf.api.*;import org.junit.jupiter.api.Test;import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TrackGraphTest {
 private TrackNode node(String id,String world,double x){return new TrackNode(id,world,new Vec3(x,64,0),12.0);}
 @Test void resolvesOnlyConnectedRoutesInOneWorld(){
  TrackGraph graph=new TrackGraph();graph.addNode(node("A","world",0));graph.addNode(node("B","world",10));graph.addNode(node("C","world",20));
  assertTrue(graph.connect("A","B"));assertTrue(graph.connect("B","C"));
  assertEquals(List.of("A","B","C"),graph.route(List.of("A","B","C")).stream().map(TrackNode::id).toList());
  assertThrows(IllegalArgumentException.class,()->graph.route(List.of("A","C")));
 }
 @Test void rejectsUnknownNodesAndCrossWorldRoutes(){
  TrackGraph graph=new TrackGraph();graph.addNode(node("A","world",0));graph.addNode(node("B","nether",10));graph.connect("A","B");
  assertThrows(IllegalArgumentException.class,()->graph.route(List.of("A","missing")));
  assertThrows(IllegalArgumentException.class,()->graph.route(List.of("A","B")));
 }
}
