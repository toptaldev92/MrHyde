package org.faudroids.mrhyde.ui.utils;

import android.graphics.Typeface;

import java.util.HashMap;
import java.util.NoSuchElementException;

import timber.log.Timber;

public class TagFactory {

    private final HashMap<String, Tag> knownTags = new HashMap<>();

    public TagFactory() {
        this.knownTags.put("*", new FontStyleTag(Typeface.ITALIC, "*"));
        this.knownTags.put("_", new FontStyleTag(Typeface.ITALIC, "_"));
        this.knownTags.put("**", new FontStyleTag(Typeface.BOLD, "**"));
        this.knownTags.put("__", new FontStyleTag(Typeface.BOLD, "__"));
        this.knownTags.put("***", new FontStyleTag(Typeface.BOLD_ITALIC, "***"));
        this.knownTags.put("___", new FontStyleTag(Typeface.BOLD_ITALIC, "___"));
    }

    public HashMap<String, Tag> getKnownTags() {
        return this.knownTags;
    }

    public Tag create(String type) {
        try {
            return this.knownTags.get(type);
        } catch (NoSuchElementException e) {
            Timber.d("Nope!");
            return null;
        }
    }
}
