package dev.pmf.engine;
import dev.pmf.api.TrackNode;
import java.util.*;
public final class TrackGraph {
 private final Map<String,TrackNode> nodes=new LinkedHashMap<>();
 private final Map<String,Set<String>> edges=new HashMap<>();
 public void addNode(TrackNode node){nodes.put(node.id(),node);edges.computeIfAbsent(node.id(),k->new LinkedHashSet<>());}
 public boolean connect(String a,String b){if(!nodes.containsKey(a)||!nodes.containsKey(b))return false;edges.computeIfAbsent(a,k->new LinkedHashSet<>()).add(b);edges.computeIfAbsent(b,k->new LinkedHashSet<>()).add(a);return true;}
 public void removeNode(String id){nodes.remove(id);edges.remove(id);for(Set<String> adjacent:edges.values())adjacent.remove(id);}
 public TrackNode node(String id){return nodes.get(id);}
 public Collection<TrackNode> nodes(){return Collections.unmodifiableCollection(nodes.values());}
 /** Finds the shortest route by rail-edge count using breadth-first search. */
 public List<TrackNode> findRoute(String start,String destination){
  if(!nodes.containsKey(start))throw new IllegalArgumentException("Unknown start node: "+start);
  if(!nodes.containsKey(destination))throw new IllegalArgumentException("Unknown destination node: "+destination);
  Map<String,String> previous=new HashMap<>();
  Deque<String> queue=new ArrayDeque<>();
  Set<String> visited=new HashSet<>();
  queue.add(start);visited.add(start);
  while(!queue.isEmpty()&&!visited.contains(destination)){
   String current=queue.removeFirst();
   for(String next:edges.getOrDefault(current,Set.of())){
    if(visited.add(next)){previous.put(next,current);queue.addLast(next);}
   }
  }
  if(!visited.contains(destination))throw new IllegalArgumentException("No connected rail route between "+start+" and "+destination);
  LinkedList<TrackNode> path=new LinkedList<>();
  String cursor=destination;
  while(cursor!=null){path.addFirst(nodes.get(cursor));cursor=previous.get(cursor);}
  String world=path.getFirst().world();
  if(path.stream().anyMatch(n->!n.world().equals(world)))throw new IllegalArgumentException("A route cannot cross worlds.");
  return List.copyOf(path);
 }
 public List<TrackNode> route(List<String> ids){
  if(ids.size()<2)throw new IllegalArgumentException("A route needs at least two nodes.");
  List<TrackNode> result=new ArrayList<>();
  for(String id:ids){TrackNode n=nodes.get(id);if(n==null)throw new IllegalArgumentException("Unknown track node: "+id);result.add(n);}
  for(int i=0;i<ids.size()-1;i++)if(!edges.getOrDefault(ids.get(i),Set.of()).contains(ids.get(i+1)))throw new IllegalArgumentException("Nodes are not connected: "+ids.get(i)+" -> "+ids.get(i+1));
  String world=result.get(0).world();if(result.stream().anyMatch(n->!n.world().equals(world)))throw new IllegalArgumentException("A route cannot cross worlds.");
  return List.copyOf(result);
 }
}
