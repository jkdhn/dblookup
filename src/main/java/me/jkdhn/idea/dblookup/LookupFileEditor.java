package me.jkdhn.idea.dblookup;

import com.intellij.database.DatabaseDataKeys;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridAppearance;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.editor.TableFileEditor;
import com.intellij.database.model.DasForeignKey;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

public class LookupFileEditor extends TableFileEditor {
    private final DataGrid grid;
    private final DataGrid sourceGrid;
    private final LookupMapping mapping;

    protected LookupFileEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull DatabaseGridDataHookUp hookUp, @NotNull DataGrid sourceGrid, @NotNull LookupMapping mapping) {
        super(project, file);
        this.grid = createDataGrid(hookUp);
        this.sourceGrid = sourceGrid;
        this.mapping = mapping;
        this.grid.getColorsScheme().setDefaultBackground(JBColor.background());
        this.grid.getEditorColorsScheme().setDefaultBackground(JBColor.background());
        DataGridUtil.addGridHeaderComponent(this.grid);
    }

    @Override
    public @NotNull DataGrid getDataGrid() {
        return grid;
    }

    public @NotNull DataGrid getSourceGrid() {
        return sourceGrid;
    }

    public @NotNull LookupMapping getMapping() {
        return mapping;
    }

    @Override
    protected void configure(@NotNull DataGrid grid, @NotNull DataGridAppearance appearance) {
        DataGridUtil.configure(grid, appearance);
        DataGridUtil.withFloatingPaging(grid, appearance);
    }

    @Override
    protected void uiDataSnapshot(@NotNull DataSink sink) {
        super.uiDataSnapshot(sink);
        DataGridUtil.uiDataSnapshot(sink, grid);
        sink.set(PlatformCoreDataKeys.FILE_EDITOR, this);
        sink.set(DatabaseDataKeys.DATA_GRID_KEY, grid);
    }
}
