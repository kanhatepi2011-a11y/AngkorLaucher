package com.movtery.angkorlauncher.launch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.multirt.Runtime;

public class RuntimeFilesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void completeJava25RuntimePassesStructureValidation() throws IOException {
        File runtime = modernRuntime();

        assertTrue(RuntimeFiles.hasRequiredStructure(runtime, 25));
    }

    @Test
    public void minecraft26Java25Arm64PreparationPassesRuntimeValidation() throws Exception {
        File runtimeHome = modernRuntime();
        Runtime runtime = runtime("Internal-25", "25", "arm64", 25);

        RuntimeFiles.validate(runtimeHome, runtime, 25, Architecture.ARCH_ARM64);
    }

    @Test
    public void partialJava25RuntimeRequiresReinstallation() throws IOException {
        File runtime = modernRuntime();
        assertTrue(new File(runtime, "lib/modules").delete());

        assertFalse(RuntimeFiles.hasRequiredStructure(runtime, 25));
    }

    @Test
    public void java8DoesNotRequireModuleImage() throws IOException {
        File runtime = temporaryFolder.newFolder("java8");
        nonEmptyFile(runtime, "release");
        nonEmptyFile(runtime, "bin/java");

        assertTrue(RuntimeFiles.hasRequiredStructure(runtime, 8));
    }

    private File modernRuntime() throws IOException {
        File runtime = temporaryFolder.newFolder("Internal-25");
        nonEmptyFile(runtime, "release");
        nonEmptyFile(runtime, "bin/java");
        nonEmptyFile(runtime, "lib/modules");
        nonEmptyFile(runtime, "lib/libjli.so");
        nonEmptyFile(runtime, "lib/server/libjvm.so");
        return runtime;
    }

    private void nonEmptyFile(File root, String path) throws IOException {
        File file = new File(root, path);
        assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(1);
        }
    }

    private Runtime runtime(String name, String version, String arch, int javaVersion)
            throws Exception {
        Constructor<Runtime> constructor = Runtime.class.getDeclaredConstructor(
                String.class, String.class, String.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(name, version, arch, javaVersion);
    }
}
