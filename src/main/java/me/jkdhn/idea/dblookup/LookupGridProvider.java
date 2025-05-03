package me.jkdhn.idea.dblookup;

import com.intellij.database.connection.throwable.info.ErrorInfo;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridListener;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DataGridUtilCore;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.datagrid.DatabaseTableGridDataHookUp;
import com.intellij.database.datagrid.DbGridDataHookUpUtil;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridDataHookUp;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRequestSource;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.GridSortingModel;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.datagrid.RowSortOrder;
import com.intellij.database.datagrid.SelectionModel;
import com.intellij.database.editor.DatabaseTableFileEditor;
import com.intellij.database.editor.OpenDataFileDescriptor;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasForeignKey;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ModelRelationManager;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.database.run.ui.grid.editors.DbGridCellEditorHelper;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.vfs.DatabaseElementVirtualFileImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

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

    public static @Nullable LookupFileEditor createEditor(Disposable parent, DataGrid sourceGrid) {
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

        DatabaseTableGridDataHookUp sourceHookUp = ObjectUtils.tryCast(DataGridUtil.getDatabaseHookUp(sourceGrid), DatabaseTableGridDataHookUp.class);
        if (sourceHookUp == null) {
            return null;
        }

        DatabaseGridDataHookUp hookUp = DbGridDataHookUpUtil.createDatabaseTableHookUp(project, parent, sourceHookUp.getSession(), sourceHookUp.getDepartment(), sourceHookUp.getVirtualFile());
        hookUp.setDatabaseTable(refTable);

        LookupMapping mapping = LookupMapping.of(key);
        LookupFileEditor fileEditor = new LookupFileEditor(project, file, hookUp, sourceGrid, mapping);

        OpenDataFileDescriptor descriptor = buildDescriptor(sourceGrid, sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getRow(sourceRow), fileEditor.getDataGrid(), mapping, file);
        if (descriptor != null) {
            String sorting = DatabaseTableFileEditor.generateSortingText(fileEditor.getDataGrid(), RowSortOrder.Type.ASC, JBIterable.from(mapping.columns()).map(c -> c.target().getName()));
            GridSortingModel<GridRow, GridColumn> sortingModel = fileEditor.getDataGrid().getDataHookup().getSortingModel();
            Document document = sortingModel == null ? null : sortingModel.getDocument();
            if (document != null) {
                ApplicationManager.getApplication().runWriteAction(() -> document.setText(sorting));
            }

            Disposable navigateOnce = Disposer.newDisposable(parent);
            hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
                @Override
                public void error(@NotNull GridRequestSource source, @NotNull ErrorInfo errorInfo) {
                }

                @Override
                public void updateCountReceived(@NotNull GridRequestSource source, int updateCount) {
                }

                @Override
                public void requestFinished(@NotNull GridRequestSource source, boolean success) {
                    descriptor.navigateTo(fileEditor.getDataGrid());
                    Disposer.dispose(navigateOnce);
                }
            }, navigateOnce);

            Disposable fixOnce = Disposer.newDisposable(parent);
            fileEditor.getDataGrid().addDataGridListener(new DataGridListener() {
                @Override
                public void onSelectionChanged(DataGrid dataGrid) {
                    SelectionModel<GridRow, GridColumn> selectionModel = dataGrid.getSelectionModel();
                    if (selectionModel.getSelectedRowCount() == 1 && selectionModel.getSelectedColumnCount() == 0) {
                        Disposer.dispose(fixOnce);
                        fixSelection(sourceGrid, dataGrid, mapping, hookUp);
                    }
                }
            }, fixOnce);
        }

        fileEditor.getDataGrid().addDataGridListener(new LookupGridListener(sourceGrid, key), parent);
        return fileEditor;
    }

    private static void fixSelection(DataGrid sourceGrid, DataGrid grid, LookupMapping mapping, DatabaseGridDataHookUp hookUp) {
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> sourceRow = sourceGrid.getSelectionModel().getLeadSelectionRow();

        GridModel<GridRow, GridColumn> model = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> row = grid.getSelectionModel().getLeadSelectionRow();

        // check if OpenDataFileDescriptor selected the correct row
        for (LookupColumnMapping column : mapping.columns()) {
            Object sourceValue = sourceModel.getValueAt(sourceRow, GridUtil.findColumn(sourceGrid, column.source().getName()));
            Object value = model.getValueAt(row, GridUtil.findColumn(grid, column.target().getName()));
            if (!DbGridCellEditorHelper.areValuesEqual(sourceValue, value, hookUp)) {
                return;
            }
        }

        // correct row was selected, select the whole row
        grid.getSelectionModel().setSelection(row, ModelIndex.forColumn(grid, 1));
        grid.getSelectionModel().selectWholeRow();
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

    private static OpenDataFileDescriptor buildDescriptor(DataGrid sourceGrid, GridRow sourceRow, DataGrid grid, LookupMapping mapping, VirtualFile file) {
        DbDataSource dataSource = DataGridUtilCore.getDatabaseSystem(grid);
        if (dataSource == null) {
            return null;
        }
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        GridColumn[] sourceColumns = JBIterable.from(mapping.columns())
                .map(c -> GridUtil.findColumn(sourceGrid, c.source().getName()))
                .map(sourceModel::getColumn)
                .toArray(new GridColumn[0]);
        Object[] sourceValues = JBIterable.of(sourceColumns)
                .map(c -> c.getValue(sourceRow))
                .toArray(new Object[0]);
        String[] targetColumns = JBIterable.from(mapping.columns())
                .map(c -> c.target().getName())
                .toArray(new String[0]);
        return new OpenDataFileDescriptor(grid.getProject(), file, targetColumns, null, List.of(new Object[][]{sourceValues}), null);
    }
}
