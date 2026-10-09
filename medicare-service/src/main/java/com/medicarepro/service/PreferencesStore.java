package com.medicarepro.service;

import com.medicarepro.model.WindowPreferences;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Serializes only window dimensions, never patient records or credentials. */
public final class PreferencesStore {
    private final Path path;

    public PreferencesStore() {
        this(Path.of(System.getProperty("user.home"), ".medicare-pro", "window-settings.ser"));
    }

    public PreferencesStore(Path path) {
        this.path = path;
    }

    public WindowPreferences load() throws IOException {
        if (!Files.exists(path)) return WindowPreferences.defaults();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            input.setObjectInputFilter(info -> {
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                return type == WindowPreferences.class || type == String.class || type == Record.class
                        ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
            });
            Object value = input.readObject();
            if (value instanceof WindowPreferences preferences) return preferences;
            throw new IOException("The saved window settings have an unexpected format.");
        } catch (ClassNotFoundException exception) {
            throw new IOException("The saved window settings cannot be read by this version.", exception);
        }
    }

    public void save(WindowPreferences preferences) throws IOException {
        Files.createDirectories(path.getParent());
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
            output.writeObject(preferences);
        }
    }
}
