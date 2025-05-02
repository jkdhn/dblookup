package me.jkdhn.idea.dblookup;

import com.intellij.database.Dbms;
import com.intellij.database.connection.throwable.info.ErrorInfo;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridAppearance;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DataGridUtilCore;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.datagrid.DbGridDataHookUpUtil;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridDataHookUp;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRequestSource;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.dialects.DatabaseDialectEx;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasForeignKey;
import com.intellij.database.model.DasNamed;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ModelRelationManager;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.database.run.ui.DataGridRequestPlace;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.util.DdlBuilder;
import com.intellij.database.vfs.DatabaseElementVirtualFileImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class LookupGridProvider {
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
            return findBestKey(project, databaseTable, selectedColumnNames) != null;
        }
        return false;
    }

    public static @Nullable DataGrid createGrid(Disposable parent, DataGrid sourceGrid) {
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

        DasForeignKey key = findBestKey(project, databaseTable, selectedColumnNames);
        if (key == null) {
            return null;
        }

        DasTable refTable = key.getRefTable();
        if (!(refTable instanceof DbElement)) {
            return null;
        }

        VirtualFile file = DbImplUtil.findDataVirtualFile((DbElement) refTable, false);
        if (!(file instanceof DatabaseElementVirtualFileImpl)) {
            return null;
        }

        DatabaseGridDataHookUp sourceHookUp = DataGridUtil.getDatabaseHookUp(sourceGrid);
        if (sourceHookUp == null) {
            return null;
        }

        DatabaseGridDataHookUp hookUp = DbGridDataHookUpUtil.createDatabaseTableHookUp(project, parent, sourceHookUp.getSession(), sourceHookUp.getDepartment(), file);
        DataGrid grid = GridUtil.createDataGrid(hookUp.getProject(), hookUp, ActionGroup.EMPTY_GROUP,
                ((BiConsumer<DataGrid, DataGridAppearance>) DataGridUtil::configure).andThen(DataGridUtil::configureFullSizeTable).andThen(GridUtil::withFloatingPaging));
        Disposer.register(parent, grid);
        if (grid.isFilteringSupported()) {
            String filter = buildFilter(sourceGrid, sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRow(sourceRow), grid, key);
            if (filter != null) {
                grid.setFilterText(filter, -1);
            }
            grid.getPanel().setSecondTopComponent(grid.getFilterComponent().getComponent());
        }
        grid.getColorsScheme().setDefaultBackground(JBColor.background());
        grid.getEditorColorsScheme().setDefaultBackground(JBColor.background());
        grid.addDataGridListener(new LookupGridListener(sourceGrid, key), parent);
        Disposer.register(parent, UiNotifyConnector.Once.installOn(grid.getPanel().getComponent(), new Activatable() {
            @Override
            public void showNotify() {
                grid.getDataHookup().getLoader().loadFirstPage(new GridRequestSource(new DataGridRequestPlace(grid)));
            }
        }));
        return grid;
    }

    private static DasForeignKey findBestKey(Project project, DasTable table, List<String> selectedColumns) {
        DasForeignKey bestKey = null;
        for (DasForeignKey key : ModelRelationManager.getForeignKeys(project, table)) {
            Set<String> columns = JBIterable.from(key.getColumnsRef().resolveObjects())
                    .filter(DasColumn.class)
                    .map(DasColumn::getName)
                    .toSet();
            if (!columns.containsAll(selectedColumns)) {
                continue;
            }
            if (bestKey == null || key.getColumnsRef().size() < bestKey.getColumnsRef().size()) {
                bestKey = key;
            }
        }
        return bestKey;
    }

    private static String buildFilter(DataGrid sourceGrid, GridRow sourceRow, DataGrid grid, DasForeignKey key) {
        DbDataSource dataSource = DataGridUtilCore.getDatabaseSystem(grid);
        if (dataSource == null) {
            return null;
        }
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        GridColumn[] sourceColumns = JBIterable.from(key.getColumnsRef().resolveObjects())
                .filter(DasColumn.class)
                .map(c -> GridUtil.findColumn(sourceGrid, c.getName()))
                .map(sourceModel::getColumn)
                .toArray(new GridColumn[0]);
        Object[] sourceValues = JBIterable.of(sourceColumns)
                .map(c -> c.getValue(sourceRow))
                .toArray(new Object[0]);
        String[] targetColumns = JBIterable.from(key.getRefColumns().resolveObjects())
                .filter(DasColumn.class)
                .map(DasNamed::getName)
                .toArray(new String[0]);
        List<String[]> formattedValues = DataGridUtil.formatValues(sourceGrid, sourceColumns, List.of(new Object[][]{sourceValues}));
        DasObject table = DataGridUtilCore.getDatabaseTable(grid);
        List<DasColumn> columns = table.getDasChildren(ObjectKind.COLUMN).filter(DasColumn.class).toList();
        Dbms dbms = DataGridUtil.getDbms(grid);
        DatabaseDialectEx dialect = DbImplUtil.getDatabaseDialect(dbms);
        DdlBuilder builder = DbImplUtil.createBuilderForUIExec(dialect, table);
        DbImplUtil.defaultWhereCondition(targetColumns, formattedValues, dataSource.getVersion())
                .accept(builder, columns, dbms);
        return builder.getStatement();
    }
}
