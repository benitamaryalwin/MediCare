package com.medicarepro.model;

public record UserSummary(long id, String username, String displayName, String role, boolean active) { }
