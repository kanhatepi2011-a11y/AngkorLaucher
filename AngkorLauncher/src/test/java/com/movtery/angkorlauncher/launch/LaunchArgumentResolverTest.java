package com.movtery.angkorlauncher.launch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import net.kdt.pojavlaunch.authenticator.microsoft.MicrosoftAuthConfig;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaunchArgumentResolverTest {
    @Test
    public void localAccountRemovesClientIdPair() {
        List<String> result = resolveLocal("--clientId", "${clientid}", "--username", "Steve");

        assertEquals(Arrays.asList("--username", "Steve"), result);
    }

    @Test
    public void localAccountRemovesXuidPair() {
        List<String> result = resolveLocal("--xuid", "${auth_xuid}", "--username", "Steve");

        assertEquals(Arrays.asList("--username", "Steve"), result);
    }

    @Test
    public void microsoftAccountUsesConfiguredClientId() {
        Map<String, String> values = new HashMap<>();
        LaunchArgumentResolver.addClientIdAliases(values, MicrosoftAuthConfig.CLIENT_ID);

        assertEquals(Arrays.asList("--clientId", MicrosoftAuthConfig.CLIENT_ID),
                LaunchArgumentResolver.resolve(
                        Arrays.asList("--clientId", "${clientid}"), values, true));
    }

    @Test
    public void microsoftAccountWithoutClientIdFailsClearly() {
        assertThrows(LaunchPreparationException.class,
                () -> LaunchArgumentResolver.resolve(
                        Arrays.asList("--clientId", "${clientid}"), new HashMap<>(), true));
    }

    @Test
    public void microsoftAccountUsesResolvedXuid() {
        Map<String, String> values = new HashMap<>();
        values.put("auth_xuid", "1234567890");

        assertEquals(Arrays.asList("--xuid", "1234567890"),
                LaunchArgumentResolver.resolve(
                        Arrays.asList("--xuid", "${auth_xuid}"), values, true));
    }

    @Test
    public void microsoftAccountWithoutXuidFailsClearly() {
        assertThrows(LoginArgumentException.class,
                () -> LaunchArgumentResolver.resolve(
                        Arrays.asList("--xuid", "${auth_xuid}"), new HashMap<>(), true));
    }

    @Test
    public void camelCaseClientIdAliasIsSupported() {
        Map<String, String> values = new HashMap<>();
        LaunchArgumentResolver.addClientIdAliases(values, MicrosoftAuthConfig.CLIENT_ID);

        assertEquals(Arrays.asList("--clientId", MicrosoftAuthConfig.CLIENT_ID),
                LaunchArgumentResolver.resolve(
                        Arrays.asList("--clientId", "${clientId}"), values, true));
    }

    @Test
    public void clientIdWithoutValueFailsForMicrosoft() {
        assertThrows(LaunchPreparationException.class,
                () -> LaunchArgumentResolver.resolve(
                        Arrays.asList("--clientId"), new HashMap<>(), true));
    }

    @Test
    public void localMissingValueDoesNotConsumeFollowingFlag() {
        assertEquals(Arrays.asList("--username", "Steve"),
                resolveLocal("--clientId", "--username", "Steve"));
    }

    @Test
    public void localMinecraft262ArgumentsContainNoClientIdPlaceholder() {
        List<String> result = resolveLocal(
                "--username", "Steve",
                "--clientId", "${clientid}",
                "--xuid", "${auth_xuid}",
                "--version", "26.2"
        );

        assertFalse(result.contains("--clientId"));
        assertFalse(result.contains("${clientid}"));
        assertFalse(result.contains("--xuid"));
        assertFalse(result.contains("${auth_xuid}"));
        JvmArgumentSanitizer.validateNoUnresolvedPlaceholders(
                result, "Local", "26.2", "game arguments");
    }

    @Test
    public void classpathPlaceholderResolvesToOneEffectiveClasspath() {
        Map<String, String> values = new HashMap<>();
        values.put("classpath", "lwjgl.jar;client.jar");
        List<String> resolved = LaunchArgumentResolver.resolve(
                Arrays.asList("-cp", "old.jar", "-classpath", "${classpath}"), values, false);

        assertEquals(Arrays.asList("-classpath", "lwjgl.jar;client.jar"),
                JvmArgumentSanitizer.keepLastSystemPropertyAndClasspath(resolved));
    }

    @Test
    public void launcherNameAndVersionResolve() {
        Map<String, String> values = new HashMap<>();
        values.put("launcher_name", "AngkorLauncher");
        values.put("launcher_version", "1.0.0");

        assertEquals(Arrays.asList(
                "-Dlauncher.name=AngkorLauncher", "-Dlauncher.version=1.0.0"),
                LaunchArgumentResolver.resolve(Arrays.asList(
                        "-Dlauncher.name=${launcher_name}",
                        "-Dlauncher.version=${launcher_version}"
                ), values, false));
    }

    @Test
    public void valuesContainingSpacesRemainSingleTokens() {
        Map<String, String> values = new HashMap<>();
        values.put("game_directory", "C:\\Games with spaces\\Minecraft");

        List<String> result = LaunchArgumentResolver.resolve(
                Arrays.asList("--gameDir", "${game_directory}"), values, false);

        assertEquals("C:\\Games with spaces\\Minecraft", result.get(1));
    }

    private List<String> resolveLocal(String... arguments) {
        return LaunchArgumentResolver.resolve(
                Arrays.asList(arguments), new HashMap<>(), false);
    }
}
