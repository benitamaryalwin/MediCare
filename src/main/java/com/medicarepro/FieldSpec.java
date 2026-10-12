package com.medicarepro;

import java.util.List;

public record FieldSpec(String key, String label, String initialValue, List<String> choices, boolean multiline) {
    public static FieldSpec text(String key, String label, String initialValue) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(), false);
    }

    public static FieldSpec choice(String key, String label, String initialValue, String... choices) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(choices), false);
    }

    public static FieldSpec multiline(String key, String label, String initialValue) {
        return new FieldSpec(key, label, initialValue == null ? "" : initialValue, List.of(), true);
    }
}
