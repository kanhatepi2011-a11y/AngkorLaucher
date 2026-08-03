package com.movtery.angkorlauncher.launch;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.multirt.Runtime;

import java.io.File;

public final class RuntimeFiles {
    private RuntimeFiles() {
    }

    public static void validate(
            File runtimeHome,
            Runtime runtime,
            int requiredJavaVersion,
            int deviceArchitecture
    ) {
        if (!hasRequiredStructure(runtimeHome, runtime.javaVersion)) {
            throw new LaunchPreparationException(
                    "The selected Java runtime is incomplete: " + runtimeHome
                            + ". Reinstall " + runtime.name + "."
            );
        }
        if (runtime.javaVersion < requiredJavaVersion) {
            throw new LaunchPreparationException(
                    "Minecraft requires Java " + requiredJavaVersion
                            + " but " + runtime.name + " provides Java " + runtime.javaVersion
            );
        }
        if (Architecture.archAsInt(runtime.arch) != deviceArchitecture) {
            throw new LaunchPreparationException(
                    "Runtime architecture " + runtime.arch
                            + " does not match device architecture "
                            + Architecture.archAsString(deviceArchitecture)
            );
        }
    }

    public static boolean hasRequiredStructure(File runtimeHome, int javaVersion) {
        if (runtimeHome == null || !runtimeHome.isDirectory()) return false;
        if (!isReadableNonEmptyFile(new File(runtimeHome, "release"))) return false;
        if (!isReadableNonEmptyFile(new File(runtimeHome, "bin/java"))) return false;
        if (javaVersion <= 8) return true;
        return isReadableNonEmptyFile(new File(runtimeHome, "lib/modules"))
                && isReadableNonEmptyFile(new File(runtimeHome, "lib/libjli.so"))
                && isReadableNonEmptyFile(new File(runtimeHome, "lib/server/libjvm.so"));
    }

    private static boolean isReadableNonEmptyFile(File file) {
        return file.isFile() && file.canRead() && file.length() > 0;
    }
}
