package com.medicarepro.service;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Observer event source used by Swing screens to refresh after record changes. */
public final class ClinicEvents {
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public void addListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void recordsChanged(String area) {
        changes.firePropertyChange("records", null, area);
    }
}
