package com.cmsbackend.services.impl;

import java.util.Locale;
import java.util.Set;

public final class HighPriorityCategories {
    private static final Set<String> CATEGORIES = Set.of(
            "gas leakage", "water pipeline burst", "power cut");

    private HighPriorityCategories() {
    }

    public static boolean includes(String category) {
        return category != null
                && CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT));
    }
}
