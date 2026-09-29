package com.prefabgoetyaltars.compat.goetydelight;

public final class GoetyDelightCompat {
    public static final String MOD_ID = "goetydelight";
    public static boolean isLoaded() { return net.neoforged.fml.ModList.get().isLoaded(MOD_ID); }
    private GoetyDelightCompat() {}
}
