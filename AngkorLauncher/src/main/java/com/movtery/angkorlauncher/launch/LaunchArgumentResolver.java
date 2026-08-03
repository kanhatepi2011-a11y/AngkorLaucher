package com.movtery.angkorlauncher.launch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LaunchArgumentResolver {
    private static final String CLIENT_ID_FLAG = "--clientId";
    private static final String XUID_FLAG = "--xuid";

    private LaunchArgumentResolver() {
    }

    public static List<String> resolve(
            List<String> input,
            Map<String, String> values,
            boolean microsoftAccount
    ) {
        ArrayList<String> result = new ArrayList<>(input.size());
        for (int index = 0; index < input.size(); index++) {
            String token = input.get(index);
            if (isMicrosoftOnlyFlag(token)) {
                if (index + 1 >= input.size() || isOption(input.get(index + 1))) {
                    if (microsoftAccount) {
                        throw new LoginArgumentException(
                                "Malformed " + token + " argument: a Microsoft account value is required"
                        );
                    }
                    continue;
                }

                String value = resolveToken(input.get(index + 1), values);
                if (value == null || value.isBlank() || containsPlaceholder(value)) {
                    if (microsoftAccount) {
                        throw new LoginArgumentException(
                                "Microsoft account argument " + token + " is required but unavailable"
                        );
                    }
                    index++;
                    continue;
                }

                result.add(token);
                result.add(value);
                index++;
                continue;
            }

            result.add(resolveToken(token, values));
        }
        return result;
    }

    static String resolveToken(String token, Map<String, String> values) {
        if (token == null) return null;
        String resolved = token;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isBlank()) continue;
            resolved = resolved.replace("${" + entry.getKey() + "}", value);
        }
        return resolved;
    }

    public static void addClientIdAliases(Map<String, String> values, String clientId) {
        if (clientId == null || clientId.isBlank()) return;
        values.put("clientid", clientId);
        values.put("clientId", clientId);
    }

    private static boolean containsPlaceholder(String token) {
        return token.contains("${");
    }

    private static boolean isMicrosoftOnlyFlag(String token) {
        return CLIENT_ID_FLAG.equals(token) || XUID_FLAG.equals(token);
    }

    private static boolean isOption(String token) {
        return token == null || token.startsWith("-");
    }
}
