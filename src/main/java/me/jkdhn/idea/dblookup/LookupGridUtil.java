package me.jkdhn.idea.dblookup;

import com.intellij.database.Dbms;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DataGridUtilCore;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.datagrid.DbGridDataHookUpUtil;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ModelRelationManager;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.util.DdlBuilder;
import com.intellij.database.util.GridTablesModel;
import com.intellij.database.vfs.DatabaseElementVirtualFileImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.TriConsumer;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class LookupGridUtil {
    public static boolean canCreateGrid(DataGrid sourceGrid) {
        Project project = sourceGrid.getProject();
        DasTable databaseTable = ObjectUtils.tryCast(DataGridUtil.getDatabaseTable(sourceGrid), DasTable.class);
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> sourceRow = sourceGrid.getSelectionModel().getLeadSelectionRow();
        if (sourceRow.isValid(sourceModel)) {
            List<String> selectedColumnNames = sourceGrid.getSelectionModel().getSelectedColumns().asIterable()
                    .map(sourceModel::getColumn)
                    .map(GridColumn::getName)
                    .toList();
            return hasAnyMapping(project, databaseTable, selectedColumnNames);
        }
        return false;
    }

    public static @Nullable LookupGridInfo resolveInfo(DataGrid sourceGrid) {
        Project project = sourceGrid.getProject();
        DasTable databaseTable = ObjectUtils.tryCast(DataGridUtil.getDatabaseTable(sourceGrid), DasTable.class);
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> sourceRow = sourceGrid.getSelectionModel().getLeadSelectionRow();
        if (!sourceRow.isValid(sourceModel)) {
            return null;
        }

        List<String> selectedColumnNames = sourceGrid.getSelectionModel().getSelectedColumns().asIterable()
                .map(sourceModel::getColumn)
                .map(GridColumn::getName)
                .toList();

        LookupMapping mapping = findBestMapping(project, databaseTable, selectedColumnNames);
        if (mapping == null) {
            return null;
        }

        DasTable targetTable = mapping.target();
        if (!(targetTable instanceof DbElement)) {
            return null;
        }

        VirtualFile file = DbImplUtil.findDataVirtualFile((DbElement) targetTable, false);
        if (!(file instanceof DatabaseElementVirtualFileImpl)) {
            return null;
        }

        DatabaseGridDataHookUp sourceHookUp = ObjectUtils.tryCast(DataGridUtil.getDatabaseHookUp(sourceGrid), DatabaseGridDataHookUp.class);
        if (sourceHookUp == null) {
            return null;
        }

        return new LookupGridInfo(
                sourceGrid,
                sourceHookUp,
                mapping,
                file,
                targetTable);
    }

    public static @NotNull LookupFileEditor createEditor(Disposable parent, LookupGridInfo info) {
        DataGrid sourceGrid = info.sourceGrid();
        Project project = sourceGrid.getProject();
        DatabaseGridDataHookUp sourceHookUp = info.sourceHookUp();
        VirtualFile file = info.file();
        DasTable targetTable = info.targetTable();

        DatabaseGridDataHookUp hookUp = DbGridDataHookUpUtil.createDatabaseTableHookUp(project, parent, sourceHookUp.getSession(), sourceHookUp.getDepartment(), file);
        hookUp.setGridTablesModel(new GridTablesModel(targetTable));

        LookupFileEditor fileEditor = new LookupFileEditor(project, file, hookUp, info);
        Disposer.register(parent, fileEditor);
        return fileEditor;
    }

    private static Stream<LookupMapping> findMappings(Project project, DasTable table, List<String> selectedColumns) {
        return ModelRelationManager.getForeignKeys(project, table).toStream()
                .map(LookupMapping::of)
                .filter(m -> m.containsAllColumns(selectedColumns));
    }

    private static LookupMapping findBestMapping(Project project, DasTable table, List<String> selectedColumns) {
        return findMappings(project, table, selectedColumns)
                .min(Comparator.comparingInt(m -> m.columns().size()))
                .orElse(null);
    }

    private static boolean hasAnyMapping(Project project, DasTable table, List<String> selectedColumns) {
        return findMappings(project, table, selectedColumns).findAny().isPresent();
    }

    public static TriConsumer<DdlBuilder, List<DasColumn>, Dbms> buildFilterCondition(DataGrid sourceGrid, GridRow sourceRow, DbDataSource dataSource, LookupMapping mapping, LookupColumnMapping primaryColumn) {
        List<LookupColumnMapping> filteredColumns = mapping.columns().stream()
                .filter(c -> !Objects.equals(c, primaryColumn))
                .toList();
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        GridColumn[] sourceColumns = JBIterable.from(filteredColumns)
                .map(c -> GridUtil.findColumn(sourceGrid, c.source().getName()))
                .map(sourceModel::getColumn)
                .toArray(new GridColumn[0]);
        Object[] sourceValues = JBIterable.of(sourceColumns)
                .map(c -> c.getValue(sourceRow))
                .toArray(new Object[0]);
        String[] targetColumns = JBIterable.from(filteredColumns)
                .map(c -> c.target().getName())
                .toArray(new String[0]);
        List<String[]> formattedValues = DataGridUtil.formatValues(sourceGrid, sourceColumns, List.of(new Object[][]{sourceValues}));
        return DbImplUtil.defaultWhereCondition(targetColumns, formattedValues, dataSource.getVersion());
    }

    public static String generateFilter(DataGrid grid, TriConsumer<DdlBuilder, List<DasColumn>, Dbms> filter) {
        DasObject table = DataGridUtilCore.getDatabaseTable(grid);
        if (table == null) {
            return "";
        }
        Dbms dbms = DataGridUtil.getDbms(grid);
        DdlBuilder where = DbImplUtil.createBuilderForUIExec(dbms, table);
        filter.accept(where, table.getDasChildren(ObjectKind.COLUMN).filter(DasColumn.class).toList(), dbms);
        if (!where.isEmpty()) {
            return where.getStatement();
        } else {
            return "";
        }
    }
}
