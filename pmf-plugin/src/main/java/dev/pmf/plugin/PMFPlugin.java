package dev.pmf.plugin;
import dev.pmf.api.*;import dev.pmf.engine.*;
import org.bukkit.*;import org.bukkit.command.*;import org.bukkit.entity.BlockDisplay;import org.bukkit.entity.Display;
import org.bukkit.plugin.java.JavaPlugin;import org.bukkit.util.Transformation;import org.joml.Vector3f;
import java.io.File;import java.sql.*;import java.util.*;
public final class PMFPlugin extends JavaPlugin implements CommandExecutor {
 private Connection db;private final TrackGraph graph=new TrackGraph();private final Map<String,RuntimeTrain> trains=new LinkedHashMap<>();private int taskId=-1;
 @Override public void onEnable(){
  saveDefaultConfig();if(!openDatabase()){getServer().getPluginManager().disablePlugin(this);return;}
  Objects.requireNonNull(getCommand("pmf")).setExecutor(this);
  taskId=Bukkit.getScheduler().scheduleSyncRepeatingTask(this,this::tick,1L,Math.max(1L,getConfig().getLong("simulation.tick-period",1L)));
  getLogger().info("PMF enabled. Use /pmf help");
 }
 @Override public void onDisable(){
  if(taskId!=-1)Bukkit.getScheduler().cancelTask(taskId);trains.values().forEach(RuntimeTrain::remove);
  try{if(db!=null&&!db.isClosed())db.close();}catch(SQLException e){getLogger().severe(e.getMessage());}
 }
 private boolean openDatabase(){
  try{
   if(!getDataFolder().exists()&&!getDataFolder().mkdirs())throw new SQLException("Cannot create plugin data folder");
   File file=new File(getDataFolder(),getConfig().getString("database.file","pmf.db"));db=DriverManager.getConnection("jdbc:sqlite:"+file.getAbsolutePath());
   try(Statement s=db.createStatement()){
    s.executeUpdate("CREATE TABLE IF NOT EXISTS track_nodes (id TEXT PRIMARY KEY, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, speed_limit REAL NOT NULL)");
    s.executeUpdate("CREATE TABLE IF NOT EXISTS track_edges (from_id TEXT NOT NULL, to_id TEXT NOT NULL, PRIMARY KEY(from_id,to_id))");
   }loadGraph();return true;
  }catch(Exception e){getLogger().severe("SQLite initialization failed: "+e.getMessage());return false;}
 }
 private void loadGraph()throws SQLException{
  try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT id,world,x,y,z,speed_limit FROM track_nodes")){
   while(r.next())graph.addNode(new TrackNode(r.getString(1),r.getString(2),new Vec3(r.getDouble(3),r.getDouble(4),r.getDouble(5)),r.getDouble(6)));
  }
  try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT from_id,to_id FROM track_edges")){while(r.next())graph.connect(r.getString(1),r.getString(2));}
 }
 private void tick(){
  double dt=getConfig().getLong("simulation.tick-period",1L)/20.0;
  for(RuntimeTrain train:trains.values()){train.sim.tick(dt);train.render();}
  trains.values().removeIf(t->{if(t.sim.finished()&&t.sim.state()==TrainState.STOPPED){t.remove();return true;}return false;});
 }
 @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof org.bukkit.entity.Player p)){sender.sendMessage("Use this command in-game.");return true;}
  if(!p.hasPermission("pmf.admin")){p.sendMessage("§cNo permission.");return true;}
  if(args.length==0||args[0].equalsIgnoreCase("help")){
   p.sendMessage("§6PMF commands");p.sendMessage("§e/pmf node add <id> [speed]");p.sendMessage("§e/pmf node connect <from> <to>");
   p.sendMessage("§e/pmf node list");p.sendMessage("§e/pmf train spawn <id> <node1,node2,...>");p.sendMessage("§e/pmf train stop <id>");p.sendMessage("§e/pmf train list");return true;
  }
  try{if(args[0].equalsIgnoreCase("node"))nodeCommand(p,args);else if(args[0].equalsIgnoreCase("train"))trainCommand(p,args);else p.sendMessage("§cUnknown command. /pmf help");}
  catch(Exception e){p.sendMessage("§cPMF: "+e.getMessage());}return true;
 }
 private void nodeCommand(org.bukkit.entity.Player p,String[] a)throws SQLException{
  if(a.length>=3&&a[1].equalsIgnoreCase("add")){
   String id=a[2];double speed=a.length>=4?Double.parseDouble(a[3]):getConfig().getDouble("simulation.max-speed-blocks-per-second",8.0);
   Location l=p.getLocation();TrackNode n=new TrackNode(id,l.getWorld().getName(),new Vec3(l.getX(),l.getY(),l.getZ()),speed);graph.addNode(n);
   try(PreparedStatement q=db.prepareStatement("INSERT OR REPLACE INTO track_nodes VALUES(?,?,?,?,?,?)")){
    q.setString(1,id);q.setString(2,n.world());q.setDouble(3,n.position().x());q.setDouble(4,n.position().y());q.setDouble(5,n.position().z());q.setDouble(6,speed);q.executeUpdate();
   }p.sendMessage("§aTrack node created: "+id);return;
  }
  if(a.length==4&&a[1].equalsIgnoreCase("connect")){
   if(!graph.connect(a[2],a[3]))throw new IllegalArgumentException("Both nodes must exist.");
   try(PreparedStatement q=db.prepareStatement("INSERT OR IGNORE INTO track_edges VALUES(?,?)")){
    q.setString(1,a[2]);q.setString(2,a[3]);q.executeUpdate();q.setString(1,a[3]);q.setString(2,a[2]);q.executeUpdate();
   }p.sendMessage("§aConnected "+a[2]+" <-> "+a[3]);return;
  }
  if(a.length==2&&a[1].equalsIgnoreCase("list")){graph.nodes().forEach(n->p.sendMessage("§e"+n.id()+" §7"+n.world()+" "+n.position()));return;}
  throw new IllegalArgumentException("Usage: /pmf node add|connect|list ...");
 }
 private void trainCommand(org.bukkit.entity.Player p,String[] a){
  if(a.length==4&&a[1].equalsIgnoreCase("spawn")){
   if(trains.containsKey(a[2]))throw new IllegalArgumentException("Train already exists.");
   List<String> ids=Arrays.stream(a[3].split(",")).map(String::trim).toList();List<TrackNode> route=graph.route(ids);
   World w=Bukkit.getWorld(route.getFirst().world());if(w==null)throw new IllegalArgumentException("Track world is not loaded.");
   TrainSimulation sim=new TrainSimulation(a[2],route,getConfig().getDouble("simulation.max-speed-blocks-per-second",8.0),getConfig().getDouble("simulation.acceleration",1.2),getConfig().getDouble("simulation.braking",2.0));
   RuntimeTrain runtime=new RuntimeTrain(sim,w);trains.put(a[2],runtime);sim.start();p.sendMessage("§aTrain spawned: "+a[2]);return;
  }
  if(a.length==3&&a[1].equalsIgnoreCase("stop")){RuntimeTrain t=trains.get(a[2]);if(t==null)throw new IllegalArgumentException("Unknown train.");t.sim.stop();p.sendMessage("§eTrain braking: "+a[2]);return;}
  if(a.length==2&&a[1].equalsIgnoreCase("list")){trains.values().forEach(t->p.sendMessage("§e"+t.sim.id()+" §7"+t.sim.state()+" §f"+String.format(Locale.US,"%.2f",t.sim.speed())+" blocks/s"));return;}
  throw new IllegalArgumentException("Usage: /pmf train spawn|stop|list ...");
 }
 private final class RuntimeTrain{
  final TrainSimulation sim;final World world;BlockDisplay body;
  RuntimeTrain(TrainSimulation sim,World world){
   this.sim=sim;this.world=world;
   if(getConfig().getBoolean("render.enabled",true))body=world.spawn(toLocation(sim.position()),BlockDisplay.class,d->{
    Material m=Material.matchMaterial(getConfig().getString("render.material","IRON_BLOCK"));d.setBlock((m==null?Material.IRON_BLOCK:m).createBlockData());
    d.setBillboard(Display.Billboard.FIXED);d.setInterpolationDuration(1);
    d.setTransformation(new Transformation(new Vector3f(0,0,0),new org.joml.AxisAngle4f(),new Vector3f(2.5f,1.2f,5.0f),new org.joml.AxisAngle4f()));d.setPersistent(false);
   });
  }
  Location toLocation(Vec3 v){return new Location(world,v.x(),v.y(),v.z());}
  void render(){if(body!=null&&body.isValid())body.teleport(toLocation(sim.position()));}
  void remove(){if(body!=null&&body.isValid())body.remove();}
 }
}
