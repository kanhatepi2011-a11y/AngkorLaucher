package com.movtery.angkorlauncher.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CacioFilesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void java8UsesLegacyDirectoryAndPrependBootClasspath() throws IOException {
        File java8 = temporaryFolder.newFolder("cacio8");
        File legacyJar = nonEmptyFile(java8, "cacio-legacy.jar");

        String result = CacioFiles.buildBootClasspath(true, java8, null, null);

        assertTrue(result.startsWith("-Xbootclasspath/p"));
        assertTrue(result.contains(legacyJar.getAbsolutePath()));
    }

    @Test
    public void java17UsesModernDirectoryAndAppendBootClasspath() throws IOException {
        ModernFiles files = modernFiles();

        String result = CacioFiles.buildBootClasspath(false, null, files.directory, files.agent);

        assertTrue(result.startsWith("-Xbootclasspath/a"));
        assertTrue(result.contains(files.agent.getAbsolutePath()));
    }

    @Test
    public void java21UsesSameOfficialModernCacioPath() throws IOException {
        ModernFiles files = modernFiles();

        String result = CacioFiles.buildBootClasspath(false, null, files.directory, files.agent);

        assertTrue(result.startsWith("-Xbootclasspath/a"));
        assertTrue(result.contains(files.agent.getAbsolutePath()));
    }

    @Test
    public void java25UsesSameOfficialModernCacioPath() throws IOException {
        ModernFiles files = modernFiles();

        String result = CacioFiles.buildBootClasspath(false, null, files.directory, files.agent);

        assertTrue(result.startsWith("-Xbootclasspath/a"));
        assertEquals(result.indexOf(files.agent.getAbsolutePath()),
                result.lastIndexOf(files.agent.getAbsolutePath()));
    }

    @Test
    public void missingModernAgentFailsClearly() throws IOException {
        File directory = temporaryFolder.newFolder("cacio17");
        nonEmptyFile(directory, "cacio-shared-1.19.1-SNAPSHOT.jar");
        nonEmptyFile(directory, "cacio-tta-1.19.1-SNAPSHOT.jar");

        assertThrows(LaunchPreparationException.class,
                () -> CacioFiles.buildBootClasspath(
                        false, null, directory, new File(directory, "cacio-agent.jar")));
    }

    private ModernFiles modernFiles() throws IOException {
        File directory = temporaryFolder.newFolder("cacio17");
        File agent = nonEmptyFile(directory, "cacio-agent.jar");
        nonEmptyFile(directory, "cacio-shared-1.19.1-SNAPSHOT.jar");
        nonEmptyFile(directory, "cacio-tta-1.19.1-SNAPSHOT.jar");
        return new ModernFiles(directory, agent);
    }

    private File nonEmptyFile(File directory, String name) throws IOException {
        File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(1);
        }
        return file;
    }

    private static final class ModernFiles {
        final File directory;
        final File agent;

        ModernFiles(File directory, File agent) {
            this.directory = directory;
            this.agent = agent;
        }
    }
}
