package net.kdt.pojavlaunch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;

public final class LWJGLClasspath {
    private LWJGLClasspath() {
    }

    public static File resolveDirectory(String gameHome) {
        if (gameHome == null || gameHome.trim().isEmpty()) {
            throw new LWJGLRuntimeException("LWJGL game home path is null or blank");
        }
        return new File(gameHome, "lwjgl3");
    }

    public static File[] findReadableJarFiles(File directory) {
        if (directory == null) {
            throw new LWJGLRuntimeException("LWJGL directory is null");
        }

        String path = directory.getAbsolutePath();
        if (!directory.exists()) {
            throw new LWJGLRuntimeException("LWJGL directory does not exist: " + path);
        }
        if (!directory.isDirectory()) {
            throw new LWJGLRuntimeException("LWJGL path is not a directory: " + path);
        }
        if (!directory.canRead()) {
            throw new LWJGLRuntimeException("LWJGL directory is unreadable: " + path);
        }

        File[] entries = directory.listFiles();
        if (entries == null) {
            throw new LWJGLRuntimeException("Unable to list LWJGL directory: " + path);
        }

        List<File> jarFiles = new ArrayList<>();
        for (File entry : entries) {
            if (entry != null
                    && entry.isFile()
                    && entry.canRead()
                    && entry.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                jarFiles.add(entry);
            }
        }

        if (jarFiles.isEmpty()) {
            throw new LWJGLRuntimeException("No readable LWJGL JAR files found in: " + path);
        }

        File[] result = jarFiles.toArray(new File[0]);
        Arrays.sort(result, Comparator.comparing(File::getName));
        return result;
    }

    public static boolean hasReadableJarFiles(File directory) {
        try {
            return findReadableJarFiles(directory).length > 0;
        } catch (LWJGLRuntimeException ignored) {
            return false;
        }
    }

    public static boolean hasMinecraft26LaunchClasses(File directory) {
        boolean hasGlfw = false;
        boolean hasVulkan = false;
        final File[] jarFiles;
        try {
            jarFiles = findReadableJarFiles(directory);
        } catch (LWJGLRuntimeException ignored) {
            return false;
        }

        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                hasGlfw |= jar.getJarEntry("org/lwjgl/glfw/GLFW.class") != null;
                hasVulkan |= jar.getJarEntry("org/lwjgl/vulkan/VK.class") != null;
            } catch (IOException ignored) {
                return false;
            }
        }
        return hasGlfw && hasVulkan;
    }

    public static String join(File[] jarFiles) {
        if (jarFiles == null || jarFiles.length == 0) {
            throw new LWJGLRuntimeException("No LWJGL JAR files were provided for the classpath");
        }

        StringBuilder classpath = new StringBuilder();
        for (File jarFile : jarFiles) {
            if (classpath.length() > 0) {
                classpath.append(File.pathSeparator);
            }
            classpath.append(jarFile.getAbsolutePath());
        }
        return classpath.toString();
    }
}
