package com.medicarepro.model;

import java.io.Serial;
import java.io.Serializable;

/** Non-sensitive desktop settings; patient information is never serialized. */
public record WindowPreferences(int width, int height) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public WindowPreferences {
        width = Math.max(900, Math.min(width, 2400));
        height = Math.max(620, Math.min(height, 1600));
    }

    public static WindowPreferences defaults() {
        return new WindowPreferences(1360, 850);
    }
}
