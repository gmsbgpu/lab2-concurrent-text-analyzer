package com.bgpu.student.lab2.util;

import java.util.Locale;

/**
 * Приводит "сырое" слово из текста к единому виду:
 *  - отбрасывает не-буквенные/не-цифровые символы по краям;
 *  - приводит к нижнему регистру.
 */
public final class WordNormalizer {

    private WordNormalizer() {
    }

    /**
     * @return нормализованное слово или null, если после нормализации
     *         ничего не осталось (например, токен был только из пунктуации).
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        int start = 0;
        int end = raw.length();

        while (start < end && !Character.isLetterOrDigit(raw.charAt(start))) {
            start++;
        }
        while (end > start && !Character.isLetterOrDigit(raw.charAt(end - 1))) {
            end--;
        }

        if (start >= end) {
            return null;
        }

        return raw.substring(start, end).toLowerCase(Locale.ROOT);
    }
}