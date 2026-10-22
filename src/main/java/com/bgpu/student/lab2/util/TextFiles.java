package com.bgpu.student.lab2.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Список входных файлов и токенизация.
 */
public final class TextFiles {

    private TextFiles() {
    }

    /** Все *.txt в каталоге (без expected-statistics.txt и distribution.txt). */
    public static List<Path> listTextFiles(Path directory) throws IOException {
        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(directory, "*.txt")) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (name.equals("expected-statistics.txt")
                        || name.equals("distribution.txt")) {
                    continue;
                }
                result.add(path);
            }
        }

        result.sort(Path::compareTo);
        return result;
    }

    /**
     * Разбивает строку на нормализованные слова.
     * Разделитель — любой символ, не являющийся буквой или цифрой.
     */
    public static List<String> tokenizeLine(String line) {
        List<String> tokens = new ArrayList<>();

        int i = 0;
        int n = line.length();
        while (i < n) {
            while (i < n && !Character.isLetterOrDigit(line.charAt(i))) {
                i++;
            }
            int start = i;
            while (i < n && Character.isLetterOrDigit(line.charAt(i))) {
                i++;
            }
            if (start < i) {
                tokens.add(line.substring(start, i));
            }
        }

        // нормализация (toLowerCase)
        tokens.replaceAll(s -> s.toLowerCase(java.util.Locale.ROOT));
        return tokens;
    }
}