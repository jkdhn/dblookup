package me.jkdhn.idea.dblookup;

import com.intellij.database.DataGridBundle;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.run.ui.CellViewer;
import com.intellij.database.run.ui.UpdateEvent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanelWithEmptyText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class LookupCellViewer implements CellViewer, Disposable.Default {
    private final DataGrid dataGrid;
    private final JComponent emptyComponent;
    private final JPanel panel;
    private Disposable currentDisposable;

    public LookupCellViewer(DataGrid dataGrid) {
        this.dataGrid = dataGrid;
        this.panel = new JPanel(new BorderLayout());
        this.emptyComponent = new JBPanelWithEmptyText().withEmptyText(DataGridBundle.message("no.cell.selected"));
        this.panel.add(emptyComponent);
        updateGrid();
    }

    private void updateGrid() {
        panel.removeAll();
        if (currentDisposable != null) {
            Disposer.dispose(currentDisposable);
        }
        currentDisposable = Disposer.newDisposable(this);
        DataGrid grid = LookupGridProvider.createGrid(currentDisposable, dataGrid);
        if (grid != null) {
            panel.add(grid.getPanel().getComponent());
        } else {
            panel.add(emptyComponent);
        }
        panel.updateUI();
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferedFocusComponent() {
        return panel;
    }

    @Override
    public void update(@Nullable UpdateEvent updateEvent) {
        if (updateEvent instanceof UpdateEvent.SelectionChanged) {
            updateGrid();
        }
    }
}
