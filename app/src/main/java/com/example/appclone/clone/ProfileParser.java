package com.example.appclone.clone;
import java.util.*;
import java.util.regex.*;

/** System profile metadata, never a guess based on display name or user ID. */
public final class ProfileParser {
    public static final class Profile {
        public final int id;
        public final String type;
        public final String name;
        Profile(int id, String type, String name) { this.id = id; this.type = type; this.name = name; }
        public String label() {
            if (type.endsWith(".CLONE")) return "Dual / Clone";
            if (type.endsWith(".MANAGED")) return "Công việc / Bảo mật";
            return "Riêng tư";
        }
    }
    public static List<Profile> parse(String dump) {
        List<Profile> result = new ArrayList<>();
        // User sections precede the device-wide properties and type defaults.
        String[] sections = dump.split("(?m)(?=^\\s*UserInfo\\{)");
        for (String section : sections) {
            Matcher header = Pattern.compile("^\\s*UserInfo\\{(\\d+):([^:}]*):[^\\n]*").matcher(section);
            if (!header.find()) continue;
            if (header.group().contains("<removing>") || header.group().contains("<partial>")) continue;
            int id = Integer.parseInt(header.group(1));
            String name = header.group(2).trim();
            if (id == 0 || !header.group().matches(".*\\bparentId=0(?:\\s.*)?$")) continue;
            Matcher type = Pattern.compile("(?m)^\\s*Type:\\s*(android\\.os\\.usertype\\.profile\\.(?:CLONE|MANAGED|PRIVATE))\\s*$").matcher(section);
            if (type.find()) result.add(new Profile(id, type.group(1), name));
        }
        result.sort(Comparator.comparingInt(p -> p.id));
        return result;
    }
}
