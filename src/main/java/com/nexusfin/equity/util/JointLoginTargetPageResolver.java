package com.nexusfin.equity.util;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class JointLoginTargetPageResolver {

    public String resolve(String scene) {
        String normalizedScene = scene == null ? "" : scene.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedScene) {
            case "push", "exercise" -> "joint-dispatch";
            case "refund" -> "joint-refund-entry";
            default -> "joint-unsupported";
        };
    }
}
