package me.jkdhn.idea.dblookup;

import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.vfs.VirtualFile;

public record LookupGridInfo(
        DataGrid sourceGrid,
        DatabaseGridDataHookUp sourceHookUp,
        LookupMapping mapping,
        VirtualFile file,
        DasTable targetTable
) {
}
