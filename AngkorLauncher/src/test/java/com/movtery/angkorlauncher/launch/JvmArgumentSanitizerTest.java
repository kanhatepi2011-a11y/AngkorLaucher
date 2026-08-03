package com.movtery.angkorlauncher.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class JvmArgumentSanitizerTest {
    @Test
    public void keepsLastPropertyAndExactlyOneClasspath() {
        List<String> result = JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(Arrays.asList(
                "-Djava.library.path=/bad",
                "-cp", "old.jar",
                "-Djava.library.path=/good",
                "-classpath", "new.jar",
                "example.Main"
        ));

        assertEquals(Arrays.asList(
                "-Djava.library.path=/good",
                "-classpath", "new.jar",
                "example.Main"
        ), result);
    }

    @Test
    public void unresolvedPropertyIsRejectedAtFinalMerge() {
        List<String> unresolved = Arrays.asList("-Dlauncher.name=${launcher_name}");

        assertThrows(LaunchPreparationException.class,
                () -> JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(unresolved));
    }

    @Test
    public void unknownOptionalLookingPlaceholderIsStillRejected() {
        List<String> unresolved = Arrays.asList("--unknown", "${unknown_value}");

        assertThrows(LaunchPreparationException.class,
                () -> JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(unresolved));
    }

    @Test
    public void pathsContainingSpacesRemainSingleArguments() {
        List<String> result = JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(Arrays.asList(
                "-Djna.tmpdir=C:\\cache with spaces\\jna", "example.Main"
        ));

        assertEquals("-Djna.tmpdir=C:\\cache with spaces\\jna", result.get(0));
    }

    @Test
    public void minecraftNativePropertiesCannotOverrideLauncherPaths() {
        List<String> result = JvmArgumentSanitizer.removeLauncherOwnedNativeProperties(Arrays.asList(
                "-Djava.library.path=/cache/natives/26.2/java",
                "-Dorg.lwjgl.librarypath=/cache/natives/26.2/java",
                "-Djna.boot.library.path=/bad",
                "-Dorg.lwjgl.system.SharedLibraryExtractPath=/data/app/read-only",
                "-Djna.tmpdir=/data/app/read-only",
                "-Dio.netty.native.workdir=/data/app/read-only",
                "-Dorg.lwjgl.opengl.libname=libmobileglues.so"
        ));

        assertEquals(Arrays.asList("-Dorg.lwjgl.opengl.libname=libmobileglues.so"), result);
    }

    @Test
    public void finalNativePathsRetainPackagedLwjglDirectory() {
        String searchPath = "/cache/natives/26.2:/data/app/angkor/lib/arm64";
        List<String> launcherArguments = Arrays.asList(
                "-Djava.library.path=" + searchPath,
                "-Dorg.lwjgl.librarypath=" + searchPath
        );
        List<String> manifestArguments = JvmArgumentSanitizer.removeLauncherOwnedNativeProperties(
                Arrays.asList("-Djava.library.path=/cache/natives/26.2/java", "example.Main"));
        java.util.ArrayList<String> merged = new java.util.ArrayList<>(launcherArguments);
        merged.addAll(manifestArguments);

        List<String> result = JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(merged);

        assertTrue(result.contains("-Djava.library.path=" + searchPath));
        assertTrue(result.contains("-Dorg.lwjgl.librarypath=" + searchPath));
        assertFalse(result.contains("-Djava.library.path=/cache/natives/26.2/java"));
    }
}
