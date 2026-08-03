package com.movtery.angkorlauncher.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class NativeWorkDirectories {
    public final File root;
    public final File lwjgl;
    public final File jna;
    public final File netty;

    private NativeWorkDirectories(File root, File lwjgl, File jna, File netty) {
        this.root = root;
        this.lwjgl = lwjgl;
        this.jna = jna;
        this.netty = netty;
    }

    public static NativeWorkDirectories prepare(File cacheDirectory) {
        if (cacheDirectory == null) {
            throw new LaunchPreparationException("The application cache directory is unavailable");
        }
        try {
            File canonicalCache = cacheDirectory.getCanonicalFile();
            File root = checkedChild(canonicalCache, "native-work");
            File lwjgl = checkedChild(root, "lwjgl");
            File jna = checkedChild(root, "jna");
            File netty = checkedChild(root, "netty");
            verifyWritableDirectory(root);
            verifyWritableDirectory(lwjgl);
            verifyWritableDirectory(jna);
            verifyWritableDirectory(netty);
            return new NativeWorkDirectories(root, lwjgl, jna, netty);
        } catch (IOException exception) {
            throw new LaunchPreparationException(
                    "Unable to prepare writable native work directories under " + cacheDirectory,
                    exception
            );
        }
    }

    private static File checkedChild(File parent, String name) throws IOException {
        File child = new File(parent, name).getCanonicalFile();
        String parentPath = parent.getCanonicalPath();
        if (!child.getPath().startsWith(parentPath + File.separator)) {
            throw new IOException("Native work directory escapes the application cache: " + child);
        }
        return child;
    }

    private static void verifyWritableDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create directory: " + directory);
        }
        if (!directory.canRead() || !directory.canWrite()) {
            throw new IOException("Directory is not readable and writable: " + directory);
        }
        File probe = File.createTempFile("write-probe-", ".tmp", directory);
        try (FileOutputStream output = new FileOutputStream(probe)) {
            output.write(1);
            output.getFD().sync();
        } finally {
            if (probe.exists() && !probe.delete()) {
                throw new IOException("Cannot remove write probe: " + probe);
            }
        }
    }
}
