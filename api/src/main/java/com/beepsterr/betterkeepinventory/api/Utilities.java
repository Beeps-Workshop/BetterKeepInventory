package com.beepsterr.betterkeepinventory.api;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Utilities {

    /**
     * A Method to compare a string with a list of strings.
     * The string can contain a couple of modifiers to change the behavior.
     * * Modifiers:
     * *   - `*` anywhere should be treated like a wildcard (regex .*)
     * *   - `!` at the start should negate the comparison
     * @return boolean
     */
    public static boolean advancedStringCompare(String input, List<String> compareAgainst)
    {
        for (String entry : compareAgainst) {
            boolean isNegated = entry.startsWith("!");
            String actual = isNegated ? entry.substring(1) : entry;

            String regex = actual.replace("*", ".*");
            Pattern pattern = Pattern.compile(regex);
            boolean matches = pattern.matcher(input).matches();

            if (isNegated != matches) {
                return true;
            }
        }
        return false;
    }

    public static List<String> ConfigList(ConfigurationSection section, String key) {
        if(section.contains(key) && section.isString(key)){
            return List.of(Objects.requireNonNull(section.getString(key)));
        }else{
            List<?> rawList = section.getList(key);
            if (rawList == null) return List.of();
            return rawList.stream()
                    .map(Object::toString)
                    .toList();
        }
    }

    public static List<String> AsList(String input) {
        return List.of(input);
    }

    public static List<?> AsList(List<?> input) {
        return input;
    }

}
