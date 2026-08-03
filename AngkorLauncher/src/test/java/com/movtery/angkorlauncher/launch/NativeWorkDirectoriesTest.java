package com.movtery.angkorlauncher.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class NativeWorkDirectoriesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsAndProbesAllDirectories() throws IOException {
        File cache = temporaryFolder.newFolder("cache with spaces");

        NativeWorkDirectories directories = NativeWorkDirectories.prepare(cache);

        assertEquals(new File(cache, "native-work").getCanonicalFile(), directories.root);
        assertTrue(directories.lwjgl.isDirectory());
        assertTrue(directories.jna.isDirectory());
        assertTrue(directories.netty.isDirectory());
        assertFalse(new File(directories.root, "write-probe.tmp").exists());
    }

    @Test
    public void fileBlockingRootFailsClearly() throws IOException {
        File cache = temporaryFolder.newFolder("cache");
        assertTrue(new File(cache, "native-work").createNewFile());

        assertThrows(LaunchPreparationException.class,
                () -> NativeWorkDirectories.prepare(cache));
    }

    @Test
    public void nullCacheFailsClearly() {
        assertThrows(LaunchPreparationException.class,
                () -> NativeWorkDirectories.prepare(null));
    }
}
