package me.jkdhn.idea.dblookup;

import com.intellij.database.DatabaseDataKeys;
import com.intellij.database.Dbms;
import com.intellij.database.connection.throwable.info.ErrorInfo;
import com.intellij.database.dataSource.DatabaseConnectionCore;
import com.intellij.database.dataSource.connection.Either;
import com.intellij.database.dataSource.connection.statements.SmartStatementFactoryService;
import com.intellij.database.dataSource.connection.statements.StagedException;
import com.intellij.database.dataSource.connection.statements.StandardExecutionMode;
import com.intellij.database.dataSource.connection.statements.StandardResultsProcessors;
import com.intellij.database.dataSource.connection.statements.StatementParameters;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridAppearance;
import com.intellij.database.datagrid.DataGridListener;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DataGridUtilCore;
import com.intellij.database.datagrid.DataRequest;
import com.intellij.database.datagrid.DatabaseGridDataHookUp;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridDataHookUp;
import com.intellij.database.datagrid.GridHelper;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRequestSource;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.GridSortingModel;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.datagrid.ModelIndexSet;
import com.intellij.database.datagrid.RowSortOrder;
import com.intellij.database.datagrid.mutating.ColumnQueryData;
import com.intellij.database.editor.DatabaseTableFileEditor;
import com.intellij.database.editor.TableFileEditor;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.database.run.ui.DataGridRequestPlace;
import com.intellij.database.run.ui.grid.editors.DbGridCellEditorHelper;
import com.intellij.database.script.generator.dml.DmlHelper;
import com.intellij.database.script.generator.dml.DmlTaskKt;
import com.intellij.database.script.generator.dml.DmlUtilKt;
import com.intellij.database.script.generator.dml.SelectTask;
import com.intellij.database.script.generator.dml.WrapInSelectResult;
import com.intellij.database.settings.DatabaseSettings;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.util.DdlBuilder;
import com.intellij.database.util.Version;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.CheckedDisposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class LookupFileEditor extends TableFileEditor {
    private final DatabaseGridDataHookUp hookUp;
    private final LookupGridInfo info;
    private final DataGrid grid;
    private CheckedDisposable currentDisposable;
    private boolean loaded;

    protected LookupFileEditor(@NotNull Project project, @NotNull VirtualFile file, @NotNull DatabaseGridDataHookUp hookUp, @NotNull LookupGridInfo info) {
        super(project, file);
        this.hookUp = hookUp;
        this.info = info;
        this.grid = createDataGrid(hookUp);
        this.grid.getColorsScheme().setDefaultBackground(JBColor.background());
        this.grid.getEditorColorsScheme().setDefaultBackground(JBColor.background());
        DataGridUtil.addGridHeaderComponent(this.grid);
        DataGridUtil.setupProgressIndicatingAuditor(this.grid);
        this.grid.addDataGridListener(new LookupGridListener(info.sourceGrid(), info.mapping()), this);
        this.hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
            @Override
            public void error(@NotNull GridRequestSource source, @NotNull ErrorInfo errorInfo) {
            }

            @Override
            public void updateCountReceived(@NotNull GridRequestSource source, int updateCount) {
            }

            @Override
            public void requestFinished(@NotNull GridRequestSource source, boolean success) {
                if (success) {
                    loaded = true;
                }
            }
        }, this);
    }

    @Override
    public @NotNull DataGrid getDataGrid() {
        return grid;
    }

    public @NotNull LookupGridInfo getGridInfo() {
        return info;
    }

    @Override
    protected void configure(@NotNull DataGrid grid, @NotNull DataGridAppearance appearance) {
        DataGridUtil.configure(grid, appearance);
        DataGridUtil.withFloatingPaging(grid, appearance);
        grid.putUserData(DatabaseDataKeys.DATA_GRID_SETTINGS_KEY, new LookupGridSettings(DatabaseSettings.getSettings()));
        grid.putUserData(GridHelper.GRID_HELPER_KEY, new LookupGridHelper(GridHelper.get(grid)));
    }

    @Override
    protected void uiDataSnapshot(@NotNull DataSink sink) {
        super.uiDataSnapshot(sink);
        DataGridUtil.uiDataSnapshot(sink, grid);
        sink.set(PlatformCoreDataKeys.FILE_EDITOR, this);
        sink.set(DatabaseDataKeys.DATA_GRID_KEY, grid);
    }

    public void configureGrid(LookupColumnMapping primaryColumn) {
        if (currentDisposable != null) {
            Disposer.dispose(currentDisposable);
        }
        currentDisposable = Disposer.newCheckedDisposable(this);

        GridModel<GridRow, GridColumn> sourceModel = info.sourceGrid().getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> sourceRow = info.sourceGrid().getSelectionModel().getLeadSelectionRow();

        String filter = LookupGridUtil.generateFilter(grid, LookupGridUtil.buildFilterCondition(info.sourceGrid(), sourceModel.getRow(sourceRow), hookUp.getDataSource(), info.mapping(), primaryColumn));
        String oldFilter = grid.getFilterText();
        grid.setFilterText(filter, -1);
        boolean needReload = !Objects.equals(oldFilter, filter);

        GridSortingModel<GridRow, GridColumn> sortingModel = hookUp.getSortingModel();
        if (sortingModel != null) {
            Document sortingDocument = sortingModel.getDocument();
            if (sortingDocument != null) {
                String text = DatabaseTableFileEditor.generateSortingText(grid, RowSortOrder.Type.ASC, JBIterable.of(primaryColumn.target().getName()));
                String oldText = WriteAction.compute(() -> {
                    String old = sortingDocument.getText();
                    sortingDocument.setText(text);
                    return old;
                });
                if (!Objects.equals(oldText, text)) {
                    needReload = true;
                }
            }
        }

        ModelIndex<GridColumn> sourceColumn = GridUtil.findColumn(info.sourceGrid(), primaryColumn.source().getName());
        Object sourceValue = sourceModel.getValueAt(sourceRow, sourceColumn);
        for (ModelIndex<GridRow> row : info.sourceGrid().getSelectionModel().getSelectedRows().asIterable()) {
            Object value = sourceModel.getValueAt(row, sourceColumn);
            if (!DbGridCellEditorHelper.areValuesEqual(sourceValue, value, hookUp)) {
                // multiple different values are selected in the source grid, don't select anything
                grid.getSelectionModel().setSelection(ModelIndexSet.forRows(grid), ModelIndexSet.forColumns(grid));
                return;
            }
        }

        if (loaded && needReload) {
            // clear the selection
            grid.getSelectionModel().setSelection(ModelIndexSet.forRows(grid), ModelIndexSet.forColumns(grid));

            // delay selecting until first page is loaded
            Disposable disposable = Disposer.newDisposable(currentDisposable);
            GridRequestSource requestSource = new GridRequestSource(new DataGridRequestPlace(grid));
            hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
                @Override
                public void error(@NotNull GridRequestSource source, @NotNull ErrorInfo errorInfo) {
                }

                @Override
                public void updateCountReceived(@NotNull GridRequestSource source, int updateCount) {
                }

                @Override
                public void requestFinished(@NotNull GridRequestSource source, boolean success) {
                    if (source == requestSource) {
                        Disposer.dispose(disposable);
                        selectValue(primaryColumn.target(), sourceValue, hookUp, currentDisposable);
                    }
                }
            }, disposable);
            hookUp.getLoader().loadFirstPage(requestSource);
            return;
        }

        selectValue(primaryColumn.target(), sourceValue, hookUp, currentDisposable);
    }

    private void selectValue(DasColumn column, Object value, DatabaseGridDataHookUp hookUp, CheckedDisposable disposable) {
        ApplicationManager.getApplication().assertIsDispatchThread();

        if (isRowSelected(info.sourceGrid(), grid, info.mapping(), hookUp)) {
            // row is already selected
            return;
        }

        // try to find the value in the currently loaded page
        if (selectRow(info.sourceGrid(), grid, info.mapping(), hookUp)) {
            // found and selected the row
            return;
        }

        if (hookUp.getSortingModel() == null || hookUp.getFilteringModel() == null) {
            // the code below only works if the grid is sorted and filtered, abort
            grid.getSelectionModel().setSelection(ModelIndexSet.forRows(grid), ModelIndexSet.forColumns(grid));
            return;
        }

        // wait for navigateToValueDelayed to complete, then select the row
        Disposable fixOnce = Disposer.newDisposable(disposable);
        grid.addDataGridListener(new DataGridListener() {
            @Override
            public void onSelectionChanged(DataGrid grid) {
                if (grid.getSelectionModel().getSelectedRowCount() == 0) {
                    return;
                }
                Disposer.dispose(fixOnce);
                selectRow(info.sourceGrid(), grid, info.mapping(), hookUp);
            }
        }, fixOnce);

        // clear the selection
        grid.getSelectionModel().setSelection(ModelIndexSet.forRows(grid), ModelIndexSet.forColumns(grid));

        // select the correct row by querying its row number from the database
        navigateToValue(column, value, disposable);
    }

    private void navigateToValue(DasColumn column, Object value, CheckedDisposable disposable) {
        if (tryNavigateToValue(column, value, disposable)) {
            return;
        }

        // columns were probably not loaded yet, try again after any request
        Disposable navigateOnce = Disposer.newDisposable(disposable);
        hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
            @Override
            public void error(@NotNull GridRequestSource source, @NotNull ErrorInfo errorInfo) {
            }

            @Override
            public void updateCountReceived(@NotNull GridRequestSource source, int updateCount) {
            }

            @Override
            public void requestFinished(@NotNull GridRequestSource source, boolean success) {
                Disposer.dispose(navigateOnce);
                tryNavigateToValue(column, value, disposable);
            }
        }, navigateOnce);
    }

    private boolean tryNavigateToValue(DasColumn column, Object value, CheckedDisposable disposable) {
        GridModel<GridRow, GridColumn> model = grid.getDataModel(DataAccessType.DATABASE_DATA);
        GridColumn gridColumn = model.getColumn(GridUtil.findColumn(grid, column.getName()));
        if (gridColumn == null) {
            return false;
        }
        navigateToRow(grid, column, gridColumn, value, grid.getFilterText(), hookUp, disposable);
        return true;
    }

    private static void navigateToRow(DataGrid grid, DasColumn primaryColumn, GridColumn primaryGridColumn, Object primaryValue, String filter, DataRequest.OwnerEx owner, CheckedDisposable disposable) {
        Project project = grid.getProject();

        DasObject table = DataGridUtilCore.getDatabaseTable(grid);
        if (table == null) {
            return;
        }

        Dbms dbms = DataGridUtil.getDbms(grid);
        DbDataSource dataSource = DataGridUtilCore.getDatabaseSystem(grid);
        Version version = DbImplUtil.getDatabaseVersion(dataSource);
        DmlHelper dmlHelper = DmlUtilKt.dmlGenerator(dbms);

        // count number of rows above the queried row to find its row number

        // build query: select all ...
        SelectTask task = DmlTaskKt.allColumns(table)
                .version(version)
                .build(DbImplUtil.createBuilderForUIExec(dbms, table));

        // ... where id is less than the queried id
        DdlBuilder builder = dmlHelper.generate(task).getBuilder();
        builder.space()
                .keyword("WHERE").space()
                .symbol("(")
                .columnRef(primaryColumn).space()
                .symbol("<").space();
        int offset = builder.length();
        builder.placeholder();

        // ... or id is null
        if (!primaryColumn.isNotNull()) {
            builder.space().keyword("OR").space().columnRef(primaryColumn).space().keywords("IS", "NULL").symbol(")");
        } else {
            builder.symbol(")");
        }

        // ... and the filter matches
        if (!filter.isEmpty()) {
            builder.space().keyword("AND").space().symbol("(").plain(filter).symbol(")");
        }

        // ... and count the rows
        WrapInSelectResult wrapResult = dmlHelper.generate(DmlTaskKt.wrapInSelect(builder.getStatement(), project)
                .countAll()
                .version(version)
                .build(DbImplUtil.createBuilderForUIExec(dbms, table)));
        String statement = wrapResult.getStatement();
        if (wrapResult.getOffset() == null) {
            return;
        }

        owner.getMessageBus().getDataProducer().processRequest(new DataRequest.RawRequest(owner) {
            @Override
            public void processRaw(Context context, DatabaseConnectionCore databaseConnectionCore) throws Exception {
                if (disposable.isDisposed()) {
                    return;
                }
                Either<StagedException, Integer> result = SmartStatementFactoryService.getInstance().poweredBy(databaseConnectionCore)
                        .parameterized()
                        .execute(new StatementParameters()
                                        .placeholdersOffsets(new int[]{offset + wrapResult.getOffset()})
                                        .parameters(List.of(new ColumnQueryData(primaryGridColumn, primaryValue)))
                                        .asData(statement),
                                StandardExecutionMode.QUERY,
                                StandardResultsProcessors.SUM);
                if (result.getLeft() != null) {
                    throw result.getLeft();
                }
                if (!disposable.isDisposed()) {
                    grid.getResultView().showFirstCell(result.rightOr(-1) + 1);
                }
            }
        });
    }

    private static ModelIndex<GridRow> findRow(DataGrid sourceGrid, DataGrid grid, LookupMapping mapping, DatabaseGridDataHookUp hookUp) {
        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndex<GridRow> sourceRow = sourceGrid.getSelectionModel().getLeadSelectionRow();
        GridModel<GridRow, GridColumn> model = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        for (ModelIndex<GridRow> row : model.getRowIndices().asIterable()) {
            if (isRow(mapping, sourceGrid, sourceModel, sourceRow, grid, model, row, hookUp)) {
                return row;
            }
        }
        return ModelIndex.forRow(model, -1);
    }

    private static boolean isRow(LookupMapping mapping, DataGrid sourceGrid, GridModel<GridRow, GridColumn> sourceModel, ModelIndex<GridRow> sourceRow, DataGrid grid, GridModel<GridRow, GridColumn> model, ModelIndex<GridRow> row, DatabaseGridDataHookUp hookUp) {
        for (LookupColumnMapping column : mapping.columns()) {
            Object sourceValue = sourceModel.getValueAt(sourceRow, GridUtil.findColumn(sourceGrid, column.source().getName()));
            Object value = model.getValueAt(row, GridUtil.findColumn(grid, column.target().getName()));
            if (!DbGridCellEditorHelper.areValuesEqual(sourceValue, value, hookUp)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRowSelected(DataGrid sourceGrid, DataGrid grid, LookupMapping mapping, DatabaseGridDataHookUp hookUp) {
        return grid.getSelectionModel().isSelectedRow(findRow(sourceGrid, grid, mapping, hookUp));
    }

    private static boolean selectRow(DataGrid sourceGrid, DataGrid grid, LookupMapping mapping, DatabaseGridDataHookUp hookUp) {
        ModelIndex<GridRow> row = findRow(sourceGrid, grid, mapping, hookUp);
        if (row.isValid(grid)) {
            grid.getSelectionModel().setSelection(row, ModelIndex.forColumn(grid, 1));
            grid.getSelectionModel().selectWholeRow();
            return true;
        } else {
            return false;
        }
    }
}
