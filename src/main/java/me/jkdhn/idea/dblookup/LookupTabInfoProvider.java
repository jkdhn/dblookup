package me.jkdhn.idea.dblookup;

import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.run.ui.CellViewer;
import com.intellij.database.run.ui.TabInfoProvider;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LookupTabInfoProvider extends TabInfoProvider {
    private final DataGrid dataGrid;
    private final CellViewer viewer;

    public LookupTabInfoProvider(@NotNull DataGrid dataGrid, @NotNull String title, @Nullable ActionGroup actionGroup) {
        super(title, actionGroup);
        this.dataGrid = dataGrid;
        this.viewer = createViewer();
        updateTabInfo();
    }

    private @NotNull CellViewer createViewer() {
        return new LookupCellViewer(dataGrid);
    }

    @Override
    public @NotNull CellViewer getViewer() {
        return viewer;
    }

    @Override
    public void dispose() {
        Disposer.dispose(viewer);
    }
}
