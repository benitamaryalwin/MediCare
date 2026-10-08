package com.medicarepro.service;

import com.medicarepro.model.WindowPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesStoreTest {
    @TempDir Path temporaryDirectory;

    @Test
    void serializesAndRestoresWindowSize() throws IOException {
        PreferencesStore store = new PreferencesStore(temporaryDirectory.resolve("settings.bin"));
        WindowPreferences expected = new WindowPreferences(1280, 780);
        store.save(expected);
        assertEquals(expected, store.load());
    }

    @Test
    void missingSettingsUseDocumentedDefaults() throws IOException {
        PreferencesStore store = new PreferencesStore(temporaryDirectory.resolve("missing.bin"));
        assertEquals(WindowPreferences.defaults(), store.load());
    }
}
