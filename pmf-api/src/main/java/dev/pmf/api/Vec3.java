package dev.pmf.api;
public record Vec3(double x, double y, double z) {
    public double distance(Vec3 o) {
        double dx=x-o.x(), dy=y-o.y(), dz=z-o.z();
        return Math.sqrt(dx*dx+dy*dy+dz*dz);
    }
    public Vec3 lerp(Vec3 o, double t) {
        return new Vec3(x+(o.x-x)*t, y+(o.y-y)*t, z+(o.z-z)*t);
    }
}
