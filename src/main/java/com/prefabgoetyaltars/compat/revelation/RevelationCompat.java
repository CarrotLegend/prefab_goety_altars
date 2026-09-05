package com.prefabgoetyaltars.compat.revelation;

public final class RevelationCompat {
    public static final String MOD_ID = "goety_revelation";
    public static boolean isLoaded() { return net.minecraftforge.fml.ModList.get().isLoaded(MOD_ID); }
    private RevelationCompat() {}
}
