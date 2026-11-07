package me.jkdhn.idea.dblookup;

import com.intellij.database.DatabaseDataKeys;
import com.intellij.database.actions.ShowEditMaximizedAction;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.run.ui.EditMaximizedView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public class LookupValueAction extends DumbAwareAction {
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (project == null || grid == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        LookupTableInfo tableInfo = CachedValuesManager.getManager(project).getCachedValue(grid, new LookupTableInfoProvider(project, grid));
        e.getPresentation().setEnabledAndVisible(LookupGridUtil.canCreateGrid(grid, tableInfo));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null) return;
        EditMaximizedView view = ShowEditMaximizedAction.getView(grid, e);
        view.open(tabInfoProvider -> tabInfoProvider instanceof LookupTabInfoProvider);
        if (grid.isEditable()) {
            JComponent focusComponent = view.getPreferedFocusComponent();
            if (focusComponent != null) focusComponent.requestFocus();
        }
    }
}
