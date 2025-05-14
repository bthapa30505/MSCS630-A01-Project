package org.shellassignment;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\S+|\"([^\"]*)\"");

    public static class ParsedCommand {
        public String name;
        public String[] args;
        public boolean background;
        public String original;

        public List<String> tokens() {
            List<String> all = new ArrayList<>();
            all.add(name);
            for (String a : args) all.add(a);
            return all;
        }
    }

    //This method is used to parse the input provided from the terminal
    public ParsedCommand parse(final String line) {
        ParsedCommand pc = new ParsedCommand();
        pc.original = line;
        List<String> parts = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(line);
        while (m.find()) {
            String token = m.group();
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }
            parts.add(token);
        }

        if (!parts.isEmpty() && parts.get(parts.size() - 1).equals("&")) {
            pc.background = true;
            parts.remove(parts.size() - 1);
        }
        pc.name = parts.get(0);
        pc.args = parts.size() > 1
                ? parts.subList(1, parts.size()).toArray(new String[0])
                : new String[0];
        return pc;
    }
}