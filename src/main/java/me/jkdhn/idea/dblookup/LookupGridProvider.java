package me.jkdhn.idea.dblookup;

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
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.datagrid.DataGridUtilCore;
import com.intellij.database.datagrid.DataRequest;
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
import com.intellij.database.datagrid.mutating.ColumnQueryData;
import com.intellij.database.editor.DatabaseTableFileEditor;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ModelRelationManager;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.database.script.generator.dml.DmlHelper;
import com.intellij.database.script.generator.dml.DmlTaskKt;
import com.intellij.database.script.generator.dml.DmlUtilKt;
import com.intellij.database.script.generator.dml.SelectTask;
import com.intellij.database.script.generator.dml.WrapInSelectResult;
import com.intellij.database.util.DbImplUtil;
import com.intellij.database.util.DdlBuilder;
import com.intellij.database.util.Version;
import com.intellij.database.vfs.DatabaseElementVirtualFileImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
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
            return findBestMapping(project, databaseTable, selectedColumnNames) != null;
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

        DatabaseTableGridDataHookUp sourceHookUp = ObjectUtils.tryCast(DataGridUtil.getDatabaseHookUp(sourceGrid), DatabaseTableGridDataHookUp.class);
        if (sourceHookUp == null) {
            return null;
        }

        LookupColumnMapping primaryColumn = mapping.columns().stream()
                .filter(c -> sourceGrid.getSelectionModel().isSelectedColumn(GridUtil.findColumn(sourceGrid, c.source().getName())))
                .findAny()
                .orElse(null);
        if (primaryColumn == null) {
            return null;
        }

        DatabaseGridDataHookUp hookUp = DbGridDataHookUpUtil.createDatabaseTableHookUp(project, parent, sourceHookUp.getSession(), sourceHookUp.getDepartment(), sourceHookUp.getVirtualFile());
        hookUp.setDatabaseTable(targetTable);

        LookupFileEditor fileEditor = new LookupFileEditor(project, file, hookUp, sourceGrid, mapping);
        Disposer.register(parent, fileEditor);

        String filter = generateFilter(fileEditor.getDataGrid(), buildFilterCondition(sourceGrid, sourceModel.getRow(sourceRow), hookUp.getDataSource(), mapping, primaryColumn));
        if (filter != null) {
            fileEditor.getDataGrid().setFilterText(filter, -1);
        }

        GridSortingModel<GridRow, GridColumn> sortingModel = hookUp.getSortingModel();
        if (sortingModel != null) {
            Document sortingDocument = sortingModel.getDocument();
            if (sortingDocument != null) {
                String text = DatabaseTableFileEditor.generateSortingText(fileEditor.getDataGrid(), RowSortOrder.Type.ASC, JBIterable.of(primaryColumn.target().getName()));
                ApplicationManager.getApplication().runWriteAction(() -> sortingDocument.setText(text));
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
                    Disposer.dispose(navigateOnce);
                    GridModel<GridRow, GridColumn> model = fileEditor.getDataGrid().getDataModel(DataAccessType.DATABASE_DATA);
                    GridColumn gridColumn = model.getColumn(GridUtil.findColumn(fileEditor.getDataGrid(), primaryColumn.target().getName()));
                    Object value = sourceModel.getValueAt(sourceRow, GridUtil.findColumn(sourceGrid, primaryColumn.source().getName()));
                    navigateToRow(fileEditor.getDataGrid(), primaryColumn.target(), gridColumn, value, filter, hookUp);
                }
            }, navigateOnce);
        }

        fileEditor.getDataGrid().addDataGridListener(new LookupGridListener(sourceGrid, mapping), parent);
        return fileEditor;
    }

    private static LookupMapping findBestMapping(Project project, DasTable table, List<String> selectedColumns) {
        return ModelRelationManager.getForeignKeys(project, table).toStream()
                .map(LookupMapping::of)
                .filter(m -> m.containsAllColumns(selectedColumns))
                .min(Comparator.comparingInt(m -> m.columns().size()))
                .orElse(null);
    }

    private static TriConsumer<DdlBuilder, List<DasColumn>, Dbms> buildFilterCondition(DataGrid sourceGrid, GridRow sourceRow, DbDataSource dataSource, LookupMapping mapping, LookupColumnMapping primaryColumn) {
        List<LookupColumnMapping> filteredColumns = mapping.columns().stream()
                .filter(c -> c != primaryColumn)
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

    private static String generateFilter(DataGrid grid, TriConsumer<DdlBuilder, List<DasColumn>, Dbms> filter) {
        DasObject table = DataGridUtilCore.getDatabaseTable(grid);
        if (table == null) {
            return null;
        }
        Dbms dbms = DataGridUtil.getDbms(grid);
        DdlBuilder where = DbImplUtil.createBuilderForUIExec(dbms, table);
        filter.accept(where, table.getDasChildren(ObjectKind.COLUMN).filter(DasColumn.class).toList(), dbms);
        if (!where.isEmpty()) {
            return where.getStatement();
        } else {
            return null;
        }
    }

    private static void navigateToRow(DataGrid grid, DasColumn primaryColumn, GridColumn primaryGridColumn, Object primaryValue, String filter, DataRequest.OwnerEx owner) {
        Project project = grid.getProject();

        DasObject table = DataGridUtilCore.getDatabaseTable(grid);
        if (table == null) {
            return;
        }

        Dbms dbms = DataGridUtil.getDbms(grid);
        DbDataSource dataSource = DataGridUtilCore.getDatabaseSystem(grid);
        Version version = DbImplUtil.getDatabaseVersion(dataSource);
        DmlHelper dmlHelper = DmlUtilKt.dmlGenerator(dbms);

        SelectTask task = DmlTaskKt.allColumns(table)
                .version(version)
                .build(DbImplUtil.createBuilderForUIExec(dbms, table));

        DdlBuilder builder = dmlHelper.generate(task).getBuilder();
        builder.space()
                .keyword("WHERE").space()
                .symbol("(")
                .columnRef(primaryColumn).space()
                .symbol("<").space();

        int offset = builder.length();
        builder.placeholder();

        if (!primaryColumn.isNotNull()) {
            builder.space().keyword("OR").space().columnRef(primaryColumn).space().keywords("IS", "NULL").symbol(")");
        } else {
            builder.symbol(")");
        }

        if (filter != null) {
            builder.space().keyword("AND").space().symbol("(").plain(filter).symbol(")");
        }

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
                ModelIndex<GridColumn> primaryIndex = GridUtil.findColumn(grid, primaryColumn.getName());
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
                grid.showCell(result.rightOr(0), primaryIndex);
            }
        });
    }
}
