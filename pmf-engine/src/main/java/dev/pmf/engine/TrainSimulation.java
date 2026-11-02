package dev.pmf.engine;
import dev.pmf.api.*;
import java.util.List;
/** Fixed-step train kinematics over a connected polyline. */
public final class TrainSimulation {
 private final String id; private final List<TrackNode> route; private int segment; private double segmentDistance,speed;
 private final double maxSpeed,acceleration,braking; private TrainState state=TrainState.IDLE;
 public TrainSimulation(String id,List<TrackNode> route,double maxSpeed,double acceleration,double braking){
  if(route.size()<2)throw new IllegalArgumentException("Route requires at least two nodes");
  this.id=id;this.route=List.copyOf(route);this.maxSpeed=maxSpeed;this.acceleration=acceleration;this.braking=braking;
 }
 public String id(){return id;} public double speed(){return speed;} public TrainState state(){return state;} public boolean finished(){return segment>=route.size()-1;}
 public void start(){if(!finished()&&state!=TrainState.FAULT)state=TrainState.RUNNING;}
 public void stop(){if(state==TrainState.RUNNING)state=TrainState.BRAKING;}
 public Vec3 position(){
  if(finished())return route.get(route.size()-1).position();
  Vec3 a=route.get(segment).position(),b=route.get(segment+1).position();double length=a.distance(b);
  return a.lerp(b,length<1.0e-9?1.0:Math.min(1.0,segmentDistance/length));
 }
 private double remainingRouteDistance(){
  if(finished())return 0;
  double remaining=Math.max(0,route.get(segment).position().distance(route.get(segment+1).position())-segmentDistance);
  for(int i=segment+1;i<route.size()-1;i++)remaining+=route.get(i).position().distance(route.get(i+1).position());
  return remaining;
 }
 public void tick(double dt){
  if(dt<=0||(state!=TrainState.RUNNING&&state!=TrainState.BRAKING))return;
  if(finished()){speed=0;state=TrainState.STOPPED;return;}
  double remaining=remainingRouteDistance(),brakeDistance=speed*speed/(2.0*Math.max(0.01,braking));
  boolean mustBrake=state==TrainState.BRAKING||remaining<=brakeDistance+0.35;
  if(mustBrake){state=TrainState.BRAKING;speed=Math.max(0,speed-braking*dt);}
  else{double limit=Math.min(maxSpeed,route.get(segment).speedLimit());speed=Math.min(limit,speed+acceleration*dt);}
  double travel=speed*dt;
  if(state==TrainState.BRAKING&&speed<=1.0e-4&&remaining>0.05){speed=0;state=TrainState.STOPPED;return;}
  while(travel>0&&!finished()){
   double length=route.get(segment).position().distance(route.get(segment+1).position());
   double left=Math.max(0,length-segmentDistance),step=Math.min(travel,left);
   segmentDistance+=step;travel-=step;
   if(segmentDistance>=length-1.0e-6){segment++;segmentDistance=0;}else break;
  }
  if(finished()){speed=0;state=TrainState.STOPPED;}
 }
}
