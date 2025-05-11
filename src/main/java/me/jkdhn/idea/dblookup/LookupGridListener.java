package me.jkdhn.idea.dblookup;

import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridListener;
import com.intellij.database.datagrid.GridColumn;
import com.intellij.database.datagrid.GridModel;
import com.intellij.database.datagrid.GridRow;
import com.intellij.database.datagrid.GridUtil;
import com.intellij.database.datagrid.ModelIndex;
import com.intellij.database.datagrid.ModelIndexSet;
import com.intellij.database.datagrid.SelectionModel;
import com.intellij.database.run.ui.DataAccessType;
import com.intellij.util.containers.JBIterable;

import java.util.List;

public class LookupGridListener implements DataGridListener {
    private final DataGrid sourceGrid;
    private final LookupMapping mapping;

    public LookupGridListener(DataGrid sourceGrid, LookupMapping mapping) {
        this.sourceGrid = sourceGrid;
        this.mapping = mapping;
    }

    @Override
    public void onSelectionChanged(DataGrid grid) {
        SelectionModel<GridRow, GridColumn> selectionModel = grid.getSelectionModel();
        ModelIndex<GridRow> row = selectionModel.getLeadSelectionRow();
        if (selectionModel.getSelectedRowCount() != 1 || selectionModel.getSelectedColumnCount() == 0) {
            return;
        }

        GridModel<GridRow, GridColumn> model = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        List<ModelIndex<GridColumn>> columns = JBIterable.from(mapping.columns())
                .map(c -> GridUtil.findColumn(grid, c.target().getName()))
                .toList();

        GridModel<GridRow, GridColumn> sourceModel = sourceGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        ModelIndexSet<GridRow> sourceRows = sourceGrid.getSelectionModel().getSelectedRows();
        List<ModelIndex<GridColumn>> sourceColumns = JBIterable.from(mapping.columns())
                .map(c -> GridUtil.findColumn(sourceGrid, c.source().getName()))
                .toList();

        for (int i = 0; i < columns.size() && i < sourceColumns.size(); i++) {
            ModelIndex<GridColumn> column = columns.get(i);
            ModelIndex<GridColumn> sourceColumn = sourceColumns.get(i);
            if (row.isValid(model) && column.isValid(model) && sourceColumn.isValid(sourceModel)) {
                sourceGrid.setCells(sourceRows, ModelIndexSet.forColumns(sourceModel, sourceColumn.asInteger()), model.getValueAt(row, column));
            }
        }
    }
}
