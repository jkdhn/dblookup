package me.jkdhn.idea.dblookup;

import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.run.ui.TabInfoProvider;
import com.intellij.database.run.ui.ValueEditorTab;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;

public class LookupValueEditorTab implements ValueEditorTab {
    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public @NotNull TabInfoProvider createTabInfoProvider(@NotNull DataGrid dataGrid, @NotNull Function0<Unit> function0) {
        return new LookupTabInfoProvider(dataGrid, "Lookup", (ActionGroup) ActionManager.getInstance().getAction("Console.TableResult.EditMaximized.Lookup.Group"));
    }
}
