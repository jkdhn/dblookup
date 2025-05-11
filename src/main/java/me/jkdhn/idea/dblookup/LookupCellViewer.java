package me.jkdhn.idea.dblookup;

import com.intellij.database.DataGridBundle;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.GridUtil;
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
import java.util.Objects;

public class LookupCellViewer implements CellViewer, Disposable.Default {
    private final LookupTabInfoProvider tabInfoProvider;
    private final DataGrid dataGrid;
    private final JComponent emptyComponent;
    private final JPanel panel;
    private LookupGridInfo currentInfo;
    private LookupFileEditor currentEditor;
    private Disposable currentDisposable;

    public LookupCellViewer(LookupTabInfoProvider tabInfoProvider, DataGrid dataGrid) {
        this.tabInfoProvider = tabInfoProvider;
        this.dataGrid = dataGrid;
        this.panel = new JPanel(new BorderLayout());
        this.emptyComponent = new JBPanelWithEmptyText().withEmptyText(DataGridBundle.message("no.cell.selected"));
        this.panel.add(emptyComponent);
        updateGrid();
    }

    private GridSettings resolveSettings(DataGrid dataGrid) {
        LookupGridInfo info = LookupGridUtil.resolveInfo(dataGrid);
        if (info == null) {
            return null;
        }


        LookupColumnMapping primaryColumn = info.mapping().columns().stream()
                .filter(c -> info.sourceGrid().getSelectionModel().isSelectedColumn(GridUtil.findColumn(info.sourceGrid(), c.source().getName())))
                .findAny()
                .orElse(null);
        if (primaryColumn == null) {
            return null;
        }

        return new GridSettings(info, primaryColumn);
    }

    private void updateGrid() {
        GridSettings settings = resolveSettings(dataGrid);

        if (settings != null && Objects.equals(settings.info(), currentInfo)) {
            if (currentEditor != null) {
                currentEditor.configureGrid(settings.primaryColumn());
            }
            return;
        }

        panel.removeAll();
        if (currentDisposable != null) {
            Disposer.dispose(currentDisposable);
        }
        currentDisposable = Disposer.newDisposable(this);

        if (settings != null) {
            currentInfo = settings.info();
            currentEditor = LookupGridUtil.createEditor(currentDisposable, settings.info());
            currentEditor.configureGrid(settings.primaryColumn());
            panel.add(currentEditor.getComponent());
            String name = currentEditor.getGridInfo().mapping().target().getName();
            tabInfoProvider.getTabInfo().setText(LookupBundle.message("EditMaximized.Lookup.text.long", name));
        } else {
            currentInfo = null;
            currentEditor = null;
            panel.add(emptyComponent);
            tabInfoProvider.getTabInfo().setText(LookupBundle.message("EditMaximized.Lookup.text"));
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

    private record GridSettings(
            LookupGridInfo info,
            LookupColumnMapping primaryColumn
    ) {
    }
}
