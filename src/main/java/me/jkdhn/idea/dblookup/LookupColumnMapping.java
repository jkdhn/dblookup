package me.jkdhn.idea.dblookup;

import com.intellij.database.model.DasColumn;

public record LookupColumnMapping(DasColumn source, DasColumn target) {
}
