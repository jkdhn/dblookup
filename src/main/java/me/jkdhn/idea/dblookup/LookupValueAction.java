package me.jkdhn.idea.dblookup;

import com.intellij.database.actions.ShowEditMaximizedAction;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.run.ui.EditMaximizedView;
import com.intellij.database.util.DataGridUIUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class LookupValueAction extends DumbAwareAction {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DataGrid grid = GridUtil.getDataGrid(e.getDataContext());
        if (grid == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (!DataGridUIUtil.inCell(grid, e)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        e.getPresentation().setEnabledAndVisible(LookupGridProvider.canCreateGrid(grid));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DataGrid grid = GridUtil.getDataGrid(e.getDataContext());
        if (grid == null) return;
        EditMaximizedView view = ShowEditMaximizedAction.getView(grid, e);
        view.open(tabInfoProvider -> tabInfoProvider instanceof LookupTabInfoProvider);
        if (grid.isEditable()) {
            JComponent focusComponent = view.getPreferedFocusComponent();
            if (focusComponent != null) focusComponent.requestFocus();
        }
    }
}
