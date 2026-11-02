package dev.pmf.plugin;

import dev.pmf.api.*;
import dev.pmf.engine.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.sql.*;
import java.util.*;

/** Paper adapter: real vanilla rail blocks, display-only visuals and server-side collision proxies. */
public final class PMFPlugin extends JavaPlugin implements CommandExecutor, Listener {
    private Connection db;
    private final TrackGraph graph = new TrackGraph();
    private final Map<String, RuntimeTrain> trains = new LinkedHashMap<>();
    private int taskId = -1;

    @Override public void onEnable() {
        saveDefaultConfig();
        if (!openDatabase()) { getServer().getPluginManager().disablePlugin(this); return; }
        Objects.requireNonNull(getCommand("pmf")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::tick, 1L,
                Math.max(1L, getConfig().getLong("simulation.tick-period", 1L)));
        getLogger().info("PMF enabled. Place vanilla rails to build track; use /pmf help.");
    }

    @Override public void onDisable() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        trains.values().forEach(RuntimeTrain::remove);
        try { if (db != null && !db.isClosed()) db.close(); }
        catch (SQLException e) { getLogger().severe(e.getMessage()); }
    }

    private boolean openDatabase() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs())
                throw new SQLException("Cannot create plugin data folder");
            File file = new File(getDataFolder(), getConfig().getString("database.file", "pmf.db"));
            db = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement s = db.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS track_nodes (id TEXT PRIMARY KEY, world TEXT NOT NULL, x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, speed_limit REAL NOT NULL)");
                s.executeUpdate("CREATE TABLE IF NOT EXISTS track_edges (from_id TEXT NOT NULL, to_id TEXT NOT NULL, PRIMARY KEY(from_id,to_id))");
            }
            loadGraph();
            return true;
        } catch (Exception e) {
            getLogger().severe("SQLite initialization failed: " + e.getMessage());
            return false;
        }
    }

    private void loadGraph() throws SQLException {
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT id,world,x,y,z,speed_limit FROM track_nodes")) {
            while (r.next()) graph.addNode(new TrackNode(r.getString(1), r.getString(2),
                    new Vec3(r.getDouble(3), r.getDouble(4), r.getDouble(5)), r.getDouble(6)));
        }
        try (Statement s = db.createStatement(); ResultSet r = s.executeQuery("SELECT from_id,to_id FROM track_edges")) {
            while (r.next()) graph.connect(r.getString(1), r.getString(2));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRailPlace(BlockPlaceEvent event) {
        if (!isRail(event.getBlockPlaced().getType())) return;
        Location location = event.getBlockPlaced().getLocation();
        Bukkit.getScheduler().runTask(this, () -> registerRail(location));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRailBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isRail(block.getType())) return;
        String id = railId(block);
        Bukkit.getScheduler().runTask(this, () -> unregisterRail(id));
    }

    /** Prevent the invisible slime collision proxies from ever damaging players or mobs. */
    @EventHandler
    public void onProxyDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Slime slime
                && "PMF collision proxy".equals(slime.getCustomName())) event.setCancelled(true);
    }

    private boolean isRail(Material type) {
        return type == Material.RAIL || type == Material.POWERED_RAIL
                || type == Material.DETECTOR_RAIL || type == Material.ACTIVATOR_RAIL;
    }

    private void registerRail(Location location) {
        Block block = location.getBlock();
        if (!isRail(block.getType())) return;
        String id = railId(block);
        if (graph.node(id) != null) return;
        double speed = getConfig().getDouble("simulation.max-speed-blocks-per-second", 8.0);
        TrackNode node = new TrackNode(id, block.getWorld().getName(),
                new Vec3(block.getX() + 0.5, block.getY() + 0.12, block.getZ() + 0.5), speed);
        try {
            graph.addNode(node);
            try (PreparedStatement q = db.prepareStatement("INSERT OR REPLACE INTO track_nodes VALUES(?,?,?,?,?,?)")) {
                q.setString(1, id); q.setString(2, node.world());
                q.setDouble(3, node.position().x()); q.setDouble(4, node.position().y());
                q.setDouble(5, node.position().z()); q.setDouble(6, speed); q.executeUpdate();
            }
            for (TrackNode other : graph.nodes()) {
                if (other.id().equals(id) || !other.id().startsWith("rail:") || !other.world().equals(node.world())) continue;
                if (node.position().distance(other.position()) <= 1.8) {
                    graph.connect(id, other.id());
                    persistEdge(id, other.id());
                    persistEdge(other.id(), id);
                }
            }
        } catch (Exception e) { getLogger().warning("Could not register rail at " + location + ": " + e.getMessage()); }
    }

    private void unregisterRail(String id) {
        graph.removeNode(id);
        try (PreparedStatement e = db.prepareStatement("DELETE FROM track_edges WHERE from_id=? OR to_id=?");
             PreparedStatement n = db.prepareStatement("DELETE FROM track_nodes WHERE id=?")) {
            e.setString(1, id); e.setString(2, id); e.executeUpdate();
            n.setString(1, id); n.executeUpdate();
        } catch (SQLException ex) { getLogger().warning("Could not remove rail node " + id + ": " + ex.getMessage()); }
        for (RuntimeTrain train : trains.values()) {
            if (train.sim.routeView().stream().anyMatch(node -> node.id().equals(id))) train.sim.emergencyStop();
        }
    }

    private String railId(Block block) {
        return "rail:" + block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private void persistEdge(String from, String to) throws SQLException {
        try (PreparedStatement q = db.prepareStatement("INSERT OR IGNORE INTO track_edges VALUES(?,?)")) {
            q.setString(1, from); q.setString(2, to); q.executeUpdate();
        }
    }

    private void tick() {
        double dt = getConfig().getLong("simulation.tick-period", 1L) / 20.0;
        for (RuntimeTrain train : trains.values()) {
            if (train.sim.state() != TrainState.FAULT && train.blockedByWorld()) {
                train.sim.emergencyStop();
                train.world.playSound(train.simLocation(), Sound.BLOCK_ANVIL_LAND, 0.25f, 1.8f);
            }
            train.sim.tick(dt);
            train.render();
        }
        trains.values().removeIf(train -> {
            if (train.sim.finished() && train.sim.state() == TrainState.STOPPED) {
                train.remove();
                return true;
            }
            return false;
        });
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("This command currently requires a player."); return true; }
        if (!p.hasPermission("pmf.admin")) { p.sendMessage("§cNo permission."); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            p.sendMessage("§6PMF commands");
            p.sendMessage("§ePlace vanilla rails directly to create track nodes.");
            p.sendMessage("§e/pmf track kit §7- get a rail-laying kit");
            p.sendMessage("§e/pmf track list §7- list saved rail node IDs");
            p.sendMessage("§e/pmf node add <id> [speed] §7- manual waypoint");
            p.sendMessage("§e/pmf node connect <from> <to>");
            p.sendMessage("§e/pmf train spawn <id> <start-node> <end-node>");
            p.sendMessage("§e/pmf train spawn <id> <node1,node2,...> §7- explicit route");
            p.sendMessage("§e/pmf train stop <id>  §e/pmf train list");
            return true;
        }
        try {
            if (args[0].equalsIgnoreCase("track")) trackCommand(p, args);
            else if (args[0].equalsIgnoreCase("node")) nodeCommand(p, args);
            else if (args[0].equalsIgnoreCase("train")) trainCommand(p, args);
            else p.sendMessage("§cUnknown command. /pmf help");
        } catch (Exception e) { p.sendMessage("§cPMF: " + e.getMessage()); }
        return true;
    }

    private void trackCommand(Player p, String[] a) {
        if (a.length == 2 && a[1].equalsIgnoreCase("kit")) {
            p.getInventory().addItem(new ItemStack(Material.RAIL, 64),
                    new ItemStack(Material.POWERED_RAIL, 32), new ItemStack(Material.REDSTONE_TORCH, 16));
            p.sendMessage("§aRail kit added. Place rails normally; PMF records and connects them.");
            return;
        }
        if (a.length == 2 && a[1].equalsIgnoreCase("list")) {
            graph.nodes().stream().filter(n -> n.id().startsWith("rail:"))
                    .forEach(n -> p.sendMessage("§e" + n.id() + " §7" + n.position()));
            return;
        }
        throw new IllegalArgumentException("Usage: /pmf track kit|list");
    }

    private void nodeCommand(Player p, String[] a) throws SQLException {
        if (a.length >= 3 && a[1].equalsIgnoreCase("add")) {
            String id = a[2];
            double speed = a.length >= 4 ? Double.parseDouble(a[3]) :
                    getConfig().getDouble("simulation.max-speed-blocks-per-second", 8.0);
            Location l = p.getLocation();
            TrackNode n = new TrackNode(id, l.getWorld().getName(), new Vec3(l.getX(), l.getY(), l.getZ()), speed);
            graph.addNode(n);
            try (PreparedStatement q = db.prepareStatement("INSERT OR REPLACE INTO track_nodes VALUES(?,?,?,?,?,?)")) {
                q.setString(1, id); q.setString(2, n.world()); q.setDouble(3, n.position().x());
                q.setDouble(4, n.position().y()); q.setDouble(5, n.position().z()); q.setDouble(6, speed);
                q.executeUpdate();
            }
            p.sendMessage("§aTrack node created: " + id); return;
        }
        if (a.length == 4 && a[1].equalsIgnoreCase("connect")) {
            if (!graph.connect(a[2], a[3])) throw new IllegalArgumentException("Both nodes must exist.");
            persistEdge(a[2], a[3]); persistEdge(a[3], a[2]);
            p.sendMessage("§aConnected " + a[2] + " <-> " + a[3]); return;
        }
        if (a.length == 2 && a[1].equalsIgnoreCase("list")) {
            graph.nodes().forEach(n -> p.sendMessage("§e" + n.id() + " §7" + n.world() + " " + n.position())); return;
        }
        throw new IllegalArgumentException("Usage: /pmf node add|connect|list ...");
    }

    private void trainCommand(Player p, String[] a) {
        if ((a.length == 4 || a.length == 5) && a[1].equalsIgnoreCase("spawn")) {
            if (trains.containsKey(a[2])) throw new IllegalArgumentException("Train already exists.");
            List<TrackNode> route;
            if (a.length == 5) route = graph.findRoute(a[3], a[4]);
            else {
                List<String> ids = Arrays.stream(a[3].split(",")).map(String::trim).toList();
                route = graph.route(ids);
            }
            World w = Bukkit.getWorld(route.getFirst().world());
            if (w == null) throw new IllegalArgumentException("Track world is not loaded.");
            TrainSimulation sim = new TrainSimulation(a[2], route,
                    getConfig().getDouble("simulation.max-speed-blocks-per-second", 8.0),
                    getConfig().getDouble("simulation.acceleration", 1.2),
                    getConfig().getDouble("simulation.braking", 2.0));
            RuntimeTrain runtime = new RuntimeTrain(sim, w);
            trains.put(a[2], runtime); sim.start();
            p.sendMessage("§aTrain spawned: " + a[2]); return;
        }
        if (a.length == 3 && a[1].equalsIgnoreCase("stop")) {
            RuntimeTrain t = trains.get(a[2]);
            if (t == null) throw new IllegalArgumentException("Unknown train.");
            t.sim.stop(); p.sendMessage("§eTrain braking: " + a[2]); return;
        }
        if (a.length == 2 && a[1].equalsIgnoreCase("list")) {
            trains.values().forEach(t -> p.sendMessage("§e" + t.sim.id() + " §7" + t.sim.state()
                    + " §f" + String.format(Locale.US, "%.2f", t.sim.speed()) + " blocks/s")); return;
        }
        throw new IllegalArgumentException("Usage: /pmf train spawn|stop|list ...");
    }

    private final class RuntimeTrain {
        final TrainSimulation sim;
        final World world;
        final List<ModelPiece> parts = new ArrayList<>();
        final List<Slime> hitboxes = new ArrayList<>();

        RuntimeTrain(TrainSimulation sim, World world) {
            this.sim = sim;
            this.world = world;
            if (!getConfig().getBoolean("render.enabled", true)) return;

            // Each carriage is assembled from separate display pieces. Dimensions are
            // deliberately kept in Minecraft blocks so the body reads as a metro car,
            // rather than a single oversized cube.
            for (int car = 0; car < 3; car++) {
                addPart(Material.POLISHED_BLACKSTONE, 0.0f, 0.10f, 0.0f, 2.28f, 0.28f, 2.92f); // underframe
                addPart(Material.LIGHT_GRAY_CONCRETE, 0.0f, 0.42f, 0.0f, 2.38f, 1.10f, 3.02f); // lower shell
                addPart(Material.RED_CONCRETE, 0.0f, 0.83f, 0.0f, 2.40f, 0.17f, 3.04f); // red waist stripe
                addPart(Material.BLACK_STAINED_GLASS, 0.0f, 1.10f, 0.0f, 2.37f, 0.52f, 2.82f); // continuous window band
                addPart(Material.LIGHT_GRAY_CONCRETE, 0.0f, 1.60f, 0.0f, 2.36f, 0.42f, 3.00f); // upper shell
                addPart(Material.SMOOTH_QUARTZ, 0.0f, 1.99f, 0.0f, 2.22f, 0.16f, 2.84f); // roof
                addPart(Material.DEEPSLATE_TILES, 0.0f, 0.03f, -0.91f, 1.48f, 0.30f, 0.66f); // bogie 1
                addPart(Material.DEEPSLATE_TILES, 0.0f, 0.03f, 0.91f, 1.48f, 0.30f, 0.66f); // bogie 2
                addPart(Material.RED_CONCRETE, 0.0f, 1.03f, -1.51f, 2.24f, 0.82f, 0.10f); // end face
                addPart(Material.RED_CONCRETE, 0.0f, 1.03f, 1.51f, 2.24f, 0.82f, 0.10f); // end face
                addPart(Material.LIGHT_BLUE_STAINED_GLASS, 0.0f, 1.23f, -1.57f, 1.25f, 0.45f, 0.035f); // end glazing
                addPart(Material.LIGHT_BLUE_STAINED_GLASS, 0.0f, 1.23f, 1.57f, 1.25f, 0.45f, 0.035f); // end glazing
            }

            for (int car = 0; car < 3; car++) {
                Slime proxy = world.spawn(simLocation(), Slime.class, slime -> {
                    slime.setSize(2);
                    slime.setAI(false);
                    slime.setAware(false);
                    slime.setSilent(true);
                    slime.setInvisible(true);
                    slime.setInvulnerable(true);
                    slime.setGravity(false);
                    slime.setCollidable(true);
                    slime.setPersistent(false);
                    slime.setRemoveWhenFarAway(false);
                    slime.setCustomName("PMF collision proxy");
                    slime.setCustomNameVisible(false);
                });
                hitboxes.add(proxy);
            }
        }

        private void addPart(Material material, float x, float y, float z,
                             float width, float height, float length) {
            Vector3f translation = new Vector3f(x, y, z);
            Vector3f scale = new Vector3f(width, height, length);
            BlockDisplay display = world.spawn(simLocation(), BlockDisplay.class, entity -> {
                entity.setBlock(material.createBlockData());
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setInterpolationDuration(1);
                entity.setPersistent(false);
                entity.setTransformation(new Transformation(new Vector3f(translation),
                        new Quaternionf(), new Vector3f(scale), new Quaternionf()));
            });
            parts.add(new ModelPiece(display, translation, scale));
        }

        Location simLocation() {
            Vec3 v = sim.position();
            return new Location(world, v.x(), v.y(), v.z());
        }

        private double yaw() {
            List<TrackNode> route = sim.routeView();
            int i = Math.min(sim.segmentIndex(), route.size() - 2);
            Vec3 a = route.get(i).position();
            Vec3 b = route.get(i + 1).position();
            return Math.atan2(b.x() - a.x(), b.z() - a.z());
        }

        boolean blockedByWorld() {
            Location base = simLocation();
            double angle = yaw();
            // Sample the full three-car envelope at underframe, window and roof height.
            for (int car = 0; car < 3; car++) {
                double offset = -car * 3.25;
                double cx = base.getX() + Math.sin(angle) * offset;
                double cz = base.getZ() + Math.cos(angle) * offset;
                for (double along : new double[]{-1.30, 0.0, 1.30}) {
                    for (double side : new double[]{-1.12, 1.12}) {
                        for (double h : new double[]{0.35, 0.95, 1.55, 2.05}) {
                            int x = (int) Math.floor(cx + Math.sin(angle) * along + Math.cos(angle) * side);
                            int z = (int) Math.floor(cz + Math.cos(angle) * along - Math.sin(angle) * side);
                            int y = (int) Math.floor(base.getY() + h);
                            if (!world.getBlockAt(x, y, z).isPassable()) return true;
                        }
                    }
                }
            }
            return false;
        }

        void render() {
            Location base = simLocation();
            double angle = yaw();
            Quaternionf rotation = new Quaternionf().rotateY((float) angle);
            for (int car = 0; car < 3; car++) {
                double offset = -car * 3.25;
                Location center = new Location(world,
                        base.getX() + Math.sin(angle) * offset,
                        base.getY(),
                        base.getZ() + Math.cos(angle) * offset);
                int first = car * 12;
                for (int i = 0; i < 12; i++) {
                    ModelPiece piece = parts.get(first + i);
                    BlockDisplay display = piece.entity();
                    if (!display.isValid()) continue;
                    display.teleport(center);
                    display.setTransformation(new Transformation(
                            new Vector3f(piece.translation()), new Quaternionf(rotation),
                            new Vector3f(piece.scale()), new Quaternionf()));
                }
                Slime proxy = hitboxes.get(car);
                if (proxy.isValid()) proxy.teleport(center.clone().add(0, 1.0, 0));
            }
        }

        void remove() {
            parts.forEach(piece -> {
                if (piece.entity().isValid()) piece.entity().remove();
            });
            hitboxes.forEach(entity -> {
                if (entity.isValid()) entity.remove();
            });
        }
    }

    private record ModelPiece(BlockDisplay entity, Vector3f translation, Vector3f scale) { }

}
