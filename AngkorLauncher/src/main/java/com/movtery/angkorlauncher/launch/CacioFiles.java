package com.movtery.angkorlauncher.launch;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public final class CacioFiles {
    private CacioFiles() {
    }

    public static String buildBootClasspath(
            boolean java8,
            File java8Directory,
            File modernDirectory,
            File modernAgent
    ) {
        File directory = java8 ? java8Directory : modernDirectory;
        if (directory == null || !directory.isDirectory() || !directory.canRead()) {
            throw missing("Caciocavallo directory", directory);
        }
        if (!java8 && (!hasModernFiles(modernDirectory) || !isReadableNonEmptyFile(modernAgent))) {
            throw missing("Caciocavallo Java agent", modernAgent);
        }

        File[] jars = directory.listFiles(file ->
                file.getName().toLowerCase().endsWith(".jar") && isReadableNonEmptyFile(file));
        if (jars == null || jars.length == 0) {
            throw missing("Caciocavallo JAR files", directory);
        }
        Arrays.sort(jars, Comparator.comparing(File::getName));

        StringBuilder classpath = new StringBuilder("-Xbootclasspath/")
                .append(java8 ? "p" : "a");
        for (File jar : jars) {
            classpath.append(File.pathSeparator).append(jar.getAbsolutePath());
        }
        return classpath.toString();
    }

    public static boolean hasModernFiles(File directory) {
        return directory != null
                && isReadableNonEmptyFile(new File(directory, "cacio-agent.jar"))
                && isReadableNonEmptyFile(new File(directory, "cacio-shared-1.19.1-SNAPSHOT.jar"))
                && isReadableNonEmptyFile(new File(directory, "cacio-tta-1.19.1-SNAPSHOT.jar"));
    }

    private static boolean isReadableNonEmptyFile(File file) {
        return file != null && file.isFile() && file.canRead() && file.length() > 0;
    }

    private static LaunchPreparationException missing(String component, File file) {
        return new LaunchPreparationException(
                component + " is missing or incomplete at " + file
                        + ". Reinstall the bundled launcher components."
        );
    }
}
