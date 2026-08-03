package com.movtery.angkorlauncher.launch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JvmArgumentSanitizer {
    private static final String[] LAUNCHER_OWNED_NATIVE_PROPERTIES = {
            "java.library.path",
            "org.lwjgl.librarypath",
            "jna.boot.library.path",
            "org.lwjgl.system.SharedLibraryExtractPath",
            "jna.tmpdir",
            "io.netty.native.workdir"
    };

    private JvmArgumentSanitizer() {
    }

    public static List<String> keepLastSystemPropertyAndClasspath(List<String> arguments) {
        Set<String> seenProperties = new HashSet<>();
        boolean seenClasspath = false;
        ArrayList<String> reversed = new ArrayList<>(arguments.size());
        for (int index = arguments.size() - 1; index >= 0; index--) {
            String argument = arguments.get(index);
            if (argument == null || argument.isBlank()) continue;
            if (index > 0 && isClasspathSwitch(arguments.get(index - 1))) {
                if (!seenClasspath) {
                    reversed.add(argument);
                    reversed.add(arguments.get(index - 1));
                    seenClasspath = true;
                }
                index--;
                continue;
            }
            if (isClasspathSwitch(argument)) continue;
            String property = systemPropertyName(argument);
            if (property == null || seenProperties.add(property)) reversed.add(argument);
        }
        ArrayList<String> result = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) result.add(reversed.get(index));
        validateNoUnresolvedPlaceholders(result, "Unknown", "Unknown", "final JVM arguments");
        return result;
    }

    public static List<String> removeClasspathArguments(List<String> arguments) {
        ArrayList<String> result = new ArrayList<>(arguments.size());
        for (int index = 0; index < arguments.size(); index++) {
            if (isClasspathSwitch(arguments.get(index))) {
                if (index + 1 < arguments.size()) index++;
            } else {
                result.add(arguments.get(index));
            }
        }
        return result;
    }

    public static List<String> removeLauncherOwnedNativeProperties(List<String> arguments) {
        ArrayList<String> result = new ArrayList<>(arguments.size());
        for (String argument : arguments) {
            if (!isLauncherOwnedNativeProperty(argument)) result.add(argument);
        }
        return result;
    }

    public static void validateNoUnresolvedPlaceholders(
            List<String> arguments,
            String accountType,
            String minecraftVersion,
            String source
    ) {
        for (int index = 0; index < arguments.size(); index++) {
            String argument = arguments.get(index);
            if (hasPlaceholder(argument)) {
                String previous = index > 0 && arguments.get(index - 1).startsWith("-")
                        ? arguments.get(index - 1) : "<none or redacted>";
                throw new LaunchPreparationException(
                        "Unresolved launch argument placeholder " + extractPlaceholder(argument)
                                + " at index " + index
                                + ", previous=" + previous
                                + ", accountType=" + accountType
                                + ", minecraftVersion=" + minecraftVersion
                                + ", source=" + source
                );
            }
        }
    }

    private static String extractPlaceholder(String argument) {
        int start = argument.indexOf("${");
        int end = argument.indexOf('}', start + 2);
        return end < 0 ? "${...}" : argument.substring(start, end + 1);
    }

    private static boolean hasPlaceholder(String argument) {
        return argument != null && argument.contains("${");
    }

    private static boolean isClasspathSwitch(String argument) {
        return "-cp".equals(argument) || "-classpath".equals(argument) || "--class-path".equals(argument);
    }

    private static boolean isLauncherOwnedNativeProperty(String argument) {
        if (argument == null) return false;
        for (String property : LAUNCHER_OWNED_NATIVE_PROPERTIES) {
            String prefix = "-D" + property;
            if (argument.equals(prefix) || argument.startsWith(prefix + "=")) return true;
        }
        return false;
    }

    private static String systemPropertyName(String argument) {
        if (!argument.startsWith("-D")) return null;
        int equals = argument.indexOf('=');
        return equals < 0 ? argument.substring(2) : argument.substring(2, equals);
    }
}
