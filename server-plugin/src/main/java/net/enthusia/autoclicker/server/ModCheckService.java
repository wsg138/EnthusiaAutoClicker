package net.enthusia.autoclicker.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;

final class ModCheckService {
    private static final List<String> MOD_MARKERS = List.of(
        "enthusia_autoclicker",
        "enthusia:autoclicker",
        "enthusia-autoclicker"
    );
    private static final List<String> LOADER_MARKERS = List.of(
        "fabric",
        "forge",
        "neoforge",
        "quilt",
        "fml"
    );

    ModCheckResult check(Player player) {
        String clientBrand = clientBrand(player);
        List<String> relevantChannels = relevantChannels(player.getListeningPluginChannels());

        boolean modDetected = containsAny(clientBrand, MOD_MARKERS)
            || relevantChannels.stream().anyMatch(channel -> containsAny(channel, MOD_MARKERS));
        if (modDetected) {
            return new ModCheckResult(
                ModCheckStatus.DETECTED,
                clientBrand,
                relevantChannels,
                "Detected an Enthusia AutoClicker client signal."
            );
        }

        boolean loaderDetected = containsAny(clientBrand, LOADER_MARKERS)
            || relevantChannels.stream().anyMatch(channel -> containsAny(channel, LOADER_MARKERS));
        if (loaderDetected) {
            return new ModCheckResult(
                ModCheckStatus.LOADER_ONLY,
                clientBrand,
                relevantChannels,
                "Detected a mod loader signal, but not this specific mod."
            );
        }

        return new ModCheckResult(
            ModCheckStatus.UNKNOWN,
            clientBrand,
            relevantChannels,
            "No server-visible signal for this specific mod was found."
        );
    }

    private String clientBrand(Player player) {
        try {
            Method method = player.getClass().getMethod("getClientBrandName");
            Object value = method.invoke(player);
            return value instanceof String brand && !brand.isBlank() ? brand : "unknown";
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return "unknown";
        }
    }

    private List<String> relevantChannels(Set<String> channels) {
        List<String> relevant = new ArrayList<>();
        for (String channel : channels) {
            if (containsAny(channel, MOD_MARKERS) || containsAny(channel, LOADER_MARKERS)) {
                relevant.add(channel);
            }
        }
        relevant.sort(Comparator.naturalOrder());
        return relevant;
    }

    private boolean containsAny(String value, List<String> markers) {
        String lower = value.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
