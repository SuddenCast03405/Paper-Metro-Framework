package dev.pmf.engine;
import dev.pmf.api.*;import org.junit.jupiter.api.Test;import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TrainSimulationTest {
 private List<TrackNode> route(){return List.of(new TrackNode("A","world",new Vec3(0,64,0),20),new TrackNode("B","world",new Vec3(100,64,0),20));}
 @Test void acceleratesAndMovesAlongRoute(){
  TrainSimulation t=new TrainSimulation("T1",route(),10,2,3);t.start();t.tick(1);
  assertEquals(2,t.speed(),1e-9);assertEquals(2,t.position().x(),1e-9);assertEquals(TrainState.RUNNING,t.state());
 }
 @Test void reachesEndAndStops(){
  TrainSimulation t=new TrainSimulation("T1",route(),10,100,100);t.start();
  for(int i=0;i<100&&t.state()!=TrainState.STOPPED;i++)t.tick(.1);
  assertEquals(TrainState.STOPPED,t.state());assertEquals(0,t.speed(),1e-9);assertEquals(100,t.position().x(),1e-6);
 }
 @Test void explicitStopTransitionsToStopped(){
  TrainSimulation t=new TrainSimulation("T1",route(),10,2,4);t.start();t.tick(1);t.stop();
  assertEquals(TrainState.BRAKING,t.state());t.tick(1);assertEquals(TrainState.STOPPED,t.state());
 }
}
