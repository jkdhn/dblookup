package me.jkdhn.idea.dblookup;

import com.intellij.database.connection.throwable.info.ErrorInfo;
import com.intellij.database.data.types.DataTypeConversion;
import com.intellij.database.datagrid.CoreGrid;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridColumnLayout;
import com.intellij.database.datagrid.GridHelper;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.dump.DumpHandler;
import com.intellij.database.dump.ExtractionHelper;
import com.intellij.database.extractors.DataExtractor;
import com.intellij.database.extractors.DataExtractorFactory;
import com.intellij.database.extractors.ExtractionConfig;
import com.intellij.database.extractors.ObjectFormatterMode;
import com.intellij.database.run.actions.DumpSource;
import com.intellij.database.run.ui.grid.GridRowComparator;
import com.intellij.database.run.ui.table.TableResultView;
import com.intellij.database.util.Out;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public class LookupGridHelper implements GridHelper {
    private final GridHelper helper;

    public LookupGridHelper(GridHelper helper) {
        this.helper = helper;
    }

    @Override
    public @Nullable VirtualFile getVirtualFile(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return null;
    }

    @Override
    public DataTypeConversion.@NotNull Builder createDataTypeConversionBuilder() {
        return helper.createDataTypeConversionBuilder();
    }

    @Override
    public @NotNull ObjectFormatterMode getDefaultMode() {
        return helper.getDefaultMode();
    }

    @Override
    public boolean canEditTogether(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull List<GridColumn> columns) {
        return helper.canEditTogether(grid, columns);
    }

    @Override
    public boolean canSortTogether(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull List<ModelIndex<GridColumn>> oldOrdering, List<ModelIndex<GridColumn>> newColumns) {
        return helper.canSortTogether(grid, oldOrdering, newColumns);
    }

    @Override
    public @Nullable GridColumn findUniqueColumn(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull List<GridColumn> columns) {
        return helper.findUniqueColumn(grid, columns);
    }

    @Override
    public @Nullable Icon getColumnIcon(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull GridColumn column, boolean forDisplay) {
        return helper.getColumnIcon(grid, column, forDisplay);
    }

    @Override
    public @NotNull JBIterable<TreeElement> getChildrenFromModel(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.getChildrenFromModel(grid);
    }

    @Override
    public @Nullable String getLocationString(@Nullable PsiElement element) {
        return helper.getLocationString(element);
    }

    @Override
    public void setFilterSortHighlighter(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull Editor editor) {
        helper.setFilterSortHighlighter(grid, editor);
    }

    @Override
    public void updateFilterSortPSI(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        helper.updateFilterSortPSI(grid);
    }

    @Override
    public void applyFix(@NotNull Project project, ErrorInfo.@NotNull Fix fix, @Nullable Object editor) {
        helper.applyFix(project, fix, editor);
    }

    @Override
    public @NotNull List<String> getUnambiguousColumnNames(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.getUnambiguousColumnNames(grid);
    }

    @Override
    public boolean canAddRow(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.canAddRow(grid);
    }

    @Override
    public boolean hasTargetForEditing(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.hasTargetForEditing(grid);
    }

    @Override
    public @Nullable String getTableName(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.getTableName(grid);
    }

    @Override
    public @Nullable String getNameForDump(@NotNull DataGrid source) {
        return helper.getNameForDump(source);
    }

    @Override
    public @Nullable String getQueryText(@NotNull DataGrid source) {
        return helper.getQueryText(source);
    }

    @Override
    public boolean isDatabaseHookUp(@NotNull DataGrid grid) {
        return helper.isDatabaseHookUp(grid);
    }

    @Override
    public int getDefaultPageSize() {
        return helper.getDefaultPageSize();
    }

    @Override
    public void setDefaultPageSize(int value) {
        helper.setDefaultPageSize(value);
    }

    @Override
    public boolean isLimitDefaultPageSize() {
        return helper.isLimitDefaultPageSize();
    }

    @Override
    public void setLimitDefaultPageSize(boolean value) {
        helper.setLimitDefaultPageSize(value);
    }

    @Override
    public @Nullable DumpSource<?> createDumpSource(@NotNull DataGrid grid, @NotNull AnActionEvent e) {
        return helper.createDumpSource(grid, e);
    }

    @Override
    public @NotNull DumpHandler<?> createDumpHandler(@NotNull DumpSource<?> source, @NotNull ExtractionHelper manager, @NotNull DataExtractorFactory factory, @NotNull ExtractionConfig config) {
        return helper.createDumpHandler(source, manager, factory, config);
    }

    @Override
    public boolean isDumpEnabled(@NotNull DumpSource<?> source) {
        return helper.isDumpEnabled(source);
    }

    @Override
    public void syncExtractorsInNotebook(@NotNull DataGrid grid, @NotNull DataExtractorFactory factory) {
        helper.syncExtractorsInNotebook(grid, factory);
    }

    @Override
    public boolean isLoadWholeTableWhenPaginationIsOff(@NotNull DataGrid grid) {
        return helper.isLoadWholeTableWhenPaginationIsOff(grid);
    }

    @Override
    public @NotNull GridColumnLayout<GridRow, GridColumn> createColumnLayout(@NotNull TableResultView resultView, @NotNull DataGrid grid) {
        return helper.createColumnLayout(resultView, grid);
    }

    @Override
    public @NlsContexts.Tooltip @Nullable String getColumnTooltipHtml(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull ModelIndex<GridColumn> columnIdx) {
        return helper.getColumnTooltipHtml(grid, columnIdx);
    }

    @Override
    public @Nullable String getDatabaseSystemName(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.getDatabaseSystemName(grid);
    }

    @Override
    public boolean isEditable(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.isEditable(grid);
    }

    @Override
    public @Nullable GridRowComparator createComparator(@NotNull GridColumn column) {
        return helper.createComparator(column);
    }

    @Override
    public @NotNull Out extractValues(@NotNull DataGrid dataGrid, @NotNull DataExtractor extractor, @NotNull Out out, boolean selection, boolean transpositionAllowed) {
        return helper.extractValues(dataGrid, extractor, out, selection, transpositionAllowed);
    }

    @Override
    public @NotNull Out extractValuesForCopy(@NotNull DataGrid dataGrid, @NotNull DataExtractor extractor, @NotNull Out out, boolean selection, boolean transpositionAllowed) {
        return helper.extractValuesForCopy(dataGrid, extractor, out, selection, transpositionAllowed);
    }

    @Override
    public boolean isColumnContainNestedTables(@Nullable GridModel<GridRow, GridColumn> gridModel, @NotNull GridColumn column) {
        return helper.isColumnContainNestedTables(gridModel, column);
    }

    @Override
    public void setFilterText(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull String text, int caretPosition) {
        helper.setFilterText(grid, text, caretPosition);
    }

    @Override
    public @Nullable Language getCellLanguage(@NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull ModelIndex<GridRow> row, @NotNull ModelIndex<GridColumn> column) {
        return helper.getCellLanguage(grid, row, column);
    }

    @Override
    public boolean canMutateColumns(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.canMutateColumns(grid);
    }

    @Override
    public @Nullable PsiCodeFragment createCellCodeFragment(@NotNull String text, @NotNull Project project, @NotNull CoreGrid<GridRow, GridColumn> grid, @NotNull ModelIndex<GridRow> row, @NotNull ModelIndex<GridColumn> column) {
        return helper.createCellCodeFragment(text, project, grid, row, column);
    }

    @Override
    public boolean isModifyColumnAcrossCollection() {
        return helper.isModifyColumnAcrossCollection();
    }

    @Override
    public boolean isMixedTypeColumns(@NotNull CoreGrid<GridRow, GridColumn> grid) {
        return helper.isMixedTypeColumns(grid);
    }

    @Override
    public boolean isSortingApplicable() {
        return helper.isSortingApplicable();
    }

    @Override
    public boolean isSortingApplicable(@NotNull ModelIndex<GridColumn> colIdx) {
        return helper.isSortingApplicable(colIdx);
    }
}
