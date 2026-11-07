package me.jkdhn.idea.dblookup;

import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.run.ui.ColumnModelModification;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.util.CachedValueProvider;
import org.jetbrains.annotations.Nullable;

public class LookupTableInfoProvider implements CachedValueProvider<LookupTableInfo> {
    private final Project project;
    private final DataGrid grid;

    public LookupTableInfoProvider(Project project, DataGrid grid) {
        this.project = project;
        this.grid = grid;
    }

    @Override
    public @Nullable Result<LookupTableInfo> compute() {
        DbPsiFacade psiFacade = DbPsiFacade.getInstance(project);
        ModificationTracker columnModelModificationTracker;
        if (grid instanceof ColumnModelModification cmm) {
            columnModelModificationTracker = cmm.getColumnModelModificationTracker();
        } else {
            columnModelModificationTracker = ModificationTracker.NEVER_CHANGED;
        }

        return Result.create(
                LookupGridUtil.resolveTableInfo(project, grid),
                psiFacade,
                columnModelModificationTracker);
    }
}
