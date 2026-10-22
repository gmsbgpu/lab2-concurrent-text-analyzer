package com.bgpu.student.lab2.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Загружает expected-statistics.txt и сравнивает с фактическим результатом.
 */
public final class StatisticsChecker {

    private StatisticsChecker() {
    }

    public static Map<String, Long> loadExpected(Path directory)
            throws IOException {
        Path file = directory.resolve("expected-statistics.txt");
        List<String> lines = Files.readAllLines(file);

        Map<String, Long> result = new HashMap<>(lines.size() * 2);

        boolean first = true;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (first) {
                first = false;
                if (line.startsWith("word")) {
                    continue; // заголовок "word count"
                }
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 2) {
                throw new IOException("Некорректная строка: " + line);
            }
            result.put(parts[0], Long.parseLong(parts[1]));
        }

        return result;
    }

    /** @return null если всё совпало, иначе текст описания ошибки. */
    public static String compare(Map<String, Long> actual,
                                 Map<String, Long> expected) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Long> e : expected.entrySet()) {
            Long got = actual.get(e.getKey());
            if (got == null) {
                sb.append("Отсутствует слово: ").append(e.getKey()).append('\n');
            } else if (!got.equals(e.getValue())) {
                sb.append("Несовпадение ").append(e.getKey())
                        .append(": expected=").append(e.getValue())
                        .append(" actual=").append(got).append('\n');
            }
        }

        for (String key : actual.keySet()) {
            if (!expected.containsKey(key)) {
                sb.append("Лишнее слово: ").append(key).append('\n');
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}