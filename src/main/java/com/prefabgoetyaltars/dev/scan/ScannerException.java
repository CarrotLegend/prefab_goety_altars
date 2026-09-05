package com.prefabgoetyaltars.dev.scan;

import net.minecraft.network.chat.Component;
import java.util.Locale;

public final class ScannerException extends IllegalArgumentException {
    public enum Reason { NO_SENDER, NO_WORLD, UNLOADED, TOO_FAR, WRONG_BLOCK, MISSING_BLOCK_ENTITY,
        INVALID_RANGE, INVALID_NAME, OUTPUT_DIRECTORY, WORLD_HEIGHT, WORLD_BORDER, WRITE_FAILED, CORRUPT_EXPORT }
    private final Reason reason;
    public ScannerException(Reason reason, String detail) { super(detail); this.reason = reason; }
    public ScannerException(Reason reason, String detail, Throwable cause) { super(detail, cause); this.reason = reason; }
    public Reason reason() { return reason; }
    public Component playerMessage() { return Component.translatable("scanner.prefab_goety_altars.error." + reason.name().toLowerCase(Locale.ROOT)); }
}
