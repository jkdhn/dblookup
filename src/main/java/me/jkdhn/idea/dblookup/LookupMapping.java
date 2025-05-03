package me.jkdhn.idea.dblookup;

import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasForeignKey;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.DasTypedObject;
import com.intellij.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record LookupMapping(DasTable source, DasTable target, List<LookupColumnMapping> columns) {
    public static LookupMapping of(DasForeignKey key) {
        Iterator<? extends DasTypedObject> refColumns = key.getRefColumns().resolveObjects().iterator();
        Iterator<? extends DasTypedObject> columnsRef = key.getColumnsRef().resolveObjects().iterator();
        List<LookupColumnMapping> columns = new ArrayList<>();
        while (refColumns.hasNext() && columnsRef.hasNext()) {
            DasColumn refColumn = ObjectUtils.tryCast(refColumns.next(), DasColumn.class);
            DasColumn columnRef = ObjectUtils.tryCast(columnsRef.next(), DasColumn.class);
            if (refColumn != null && columnRef != null) {
                columns.add(new LookupColumnMapping(columnRef, refColumn));
            }
        }
        return new LookupMapping(key.getTable(), key.getRefTable(), columns);
    }
}
