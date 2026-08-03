package net.kdt.pojavlaunch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class LWJGLClasspathTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void missingDirectoryFailsClearly() {
        File missing = new File(temporaryFolder.getRoot(), "missing");
        assertThrows(LWJGLRuntimeException.class,
                () -> Tools.getLWJGL3ClassPath(missing));
    }

    @Test
    public void unreadableDirectoryFailsClearly() {
        File unreadable = fakeDirectory(false, new File[0]);
        assertThrows(LWJGLRuntimeException.class,
                () -> Tools.getLWJGL3ClassPath(unreadable));
    }

    @Test
    public void nullDirectoryListingFailsClearly() {
        File unlistable = fakeDirectory(true, null);
        assertThrows(LWJGLRuntimeException.class,
                () -> Tools.getLWJGL3ClassPath(unlistable));
    }

    @Test
    public void emptyDirectoryFailsInsteadOfRemovingSeparator() throws IOException {
        File empty = temporaryFolder.newFolder("empty");
        assertThrows(LWJGLRuntimeException.class,
                () -> Tools.getLWJGL3ClassPath(empty));
    }

    @Test
    public void oneJarHasNoTrailingSeparator() throws IOException {
        File directory = temporaryFolder.newFolder("one");
        File jar = new File(directory, "lwjgl.jar");
        jar.createNewFile();

        String classpath = classpathFor(directory);

        assertEquals(jar.getAbsolutePath(), classpath);
        assertFalse(classpath.endsWith(File.pathSeparator));
    }

    @Test
    public void multipleJarsAreSortedAndJoined() throws IOException {
        File directory = temporaryFolder.newFolder("multiple");
        File second = new File(directory, "b.jar");
        File first = new File(directory, "a.jar");
        second.createNewFile();
        first.createNewFile();

        assertEquals(first.getAbsolutePath() + File.pathSeparator + second.getAbsolutePath(),
                classpathFor(directory));
    }

    @Test
    public void nonJarFilesAreIgnoredCaseInsensitively() throws IOException {
        File directory = temporaryFolder.newFolder("mixed");
        File jar = new File(directory, "LWJGL.JAR");
        jar.createNewFile();
        new File(directory, "version").createNewFile();
        new File(directory, "notes.txt").createNewFile();

        assertEquals(jar.getAbsolutePath(), classpathFor(directory));
    }

    @Test
    public void unreadableJarFilesAreIgnored() throws IOException {
        File directory = temporaryFolder.newFolder("unreadable jar");
        File readableJar = new File(directory, "readable.jar");
        readableJar.createNewFile();
        File unreadableJar = new File(directory, "blocked.jar") {
            @Override public boolean isFile() { return true; }
            @Override public boolean canRead() { return false; }
        };
        File fakeDirectory = fakeDirectory(true, new File[] { unreadableJar, readableJar });

        assertEquals(readableJar.getAbsolutePath(), classpathFor(fakeDirectory));
    }

    @Test
    public void pathsContainingSpacesArePreserved() throws IOException {
        File directory = temporaryFolder.newFolder("runtime with spaces");
        File jar = new File(directory, "lwjgl glfw.jar");
        jar.createNewFile();

        assertEquals(jar.getAbsolutePath(), classpathFor(directory));
    }

    @Test
    public void blankGameHomeFailsClearly() {
        assertThrows(LWJGLRuntimeException.class,
                () -> LWJGLClasspath.resolveDirectory("  "));
        assertThrows(LWJGLRuntimeException.class,
                () -> LWJGLClasspath.resolveDirectory(null));
    }

    @Test
    public void oldLwjglJarWithoutVulkanClassesRequiresReinstall() throws IOException {
        File directory = temporaryFolder.newFolder("old lwjgl");
        createJar(new File(directory, "lwjgl-glfw-classes.jar"),
                "org/lwjgl/glfw/GLFW.class");

        assertFalse(LWJGLClasspath.hasMinecraft26LaunchClasses(directory));
    }

    @Test
    public void minecraft26LwjglClassesPassComponentCheck() throws IOException {
        File directory = temporaryFolder.newFolder("minecraft 26 lwjgl");
        createJar(new File(directory, "lwjgl-glfw-classes.jar"),
                "org/lwjgl/glfw/GLFW.class",
                "org/lwjgl/vulkan/VK.class");

        assertTrue(LWJGLClasspath.hasMinecraft26LaunchClasses(directory));
    }

    private String classpathFor(File directory) {
        return Tools.getLWJGL3ClassPath(directory);
    }

    private File fakeDirectory(boolean readable, File[] listing) {
        return new File(temporaryFolder.getRoot(), "fake") {
            @Override public boolean exists() { return true; }
            @Override public boolean isDirectory() { return true; }
            @Override public boolean canRead() { return readable; }
            @Override public File[] listFiles() { return listing; }
            @Override public File[] listFiles(FileFilter filter) { return listing; }
        };
    }

    private void createJar(File file, String... entries) throws IOException {
        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
            for (String entry : entries) {
                output.putNextEntry(new JarEntry(entry));
                output.write(1);
                output.closeEntry();
            }
        }
    }
}
