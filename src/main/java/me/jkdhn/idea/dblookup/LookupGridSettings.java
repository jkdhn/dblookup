package me.jkdhn.idea.dblookup;

import com.intellij.database.settings.DataGridSettings;
import com.intellij.openapi.util.ModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.util.List;

public class LookupGridSettings implements DataGridSettings {
    private final DataGridSettings settings;

    public LookupGridSettings(DataGridSettings settings) {
        this.settings = settings;
    }

    @Override
    public void setEnablePagingInInEditorResultsByDefault(boolean enablePagingInInEditorResultsByDefault) {
        settings.setEnablePagingInInEditorResultsByDefault(enablePagingInInEditorResultsByDefault);
    }

    @Override
    public boolean isEnablePagingInInEditorResultsByDefault() {
        return settings.isEnablePagingInInEditorResultsByDefault();
    }

    @Override
    public boolean isDetectTextInBinaryColumns() {
        return settings.isDetectTextInBinaryColumns();
    }

    @Override
    public boolean isDetectUUIDInBinaryColumns() {
        return settings.isDetectUUIDInBinaryColumns();
    }

    @Override
    public boolean isAddToSortViaAltClick() {
        return settings.isAddToSortViaAltClick();
    }

    @Override
    public void setAddToSortViaAltClick(boolean value) {
        settings.setAddToSortViaAltClick(value);
    }

    @Override
    public void setAutoTransposeMode(@NotNull AutoTransposeMode autoTransposeMode) {
        settings.setAutoTransposeMode(autoTransposeMode);
    }

    @Override
    public @NotNull AutoTransposeMode getAutoTransposeMode() {
        return settings.getAutoTransposeMode();
    }

    @Override
    public void setEnableLocalFilterByDefault(boolean enableLocalFilterByDefault) {
        settings.setEnableLocalFilterByDefault(enableLocalFilterByDefault);
    }

    @Override
    public boolean isEnableLocalFilterByDefault() {
        return settings.isEnableLocalFilterByDefault();
    }

    @Override
    public boolean isDisableGridFloatingToolbar() {
        return true;
    }

    @Override
    public void setDisableGridFloatingToolbar(boolean disableGridFloatingToolbar) {
    }

    @Override
    public @NotNull PagingDisplayMode getPagingDisplayMode() {
        return settings.getPagingDisplayMode();
    }

    @Override
    public void setPagingDisplayMode(@NotNull PagingDisplayMode pagingDisplayMode) {
        settings.setPagingDisplayMode(pagingDisplayMode);
    }

    @Override
    public boolean isEnableImmediateCompletionInGridCells() {
        return settings.isEnableImmediateCompletionInGridCells();
    }

    @Override
    public void setEnableImmediateCompletionInGridCells(boolean enableImmediateCompletionInGridCells) {
        settings.setEnableImmediateCompletionInGridCells(enableImmediateCompletionInGridCells);
    }

    @Override
    public int getBytesLimitPerValue() {
        return settings.getBytesLimitPerValue();
    }

    @Override
    public int getFiltersHistorySize() {
        return settings.getFiltersHistorySize();
    }

    @Override
    public void setBytesLimitPerValue(int value) {
        settings.setBytesLimitPerValue(value);
    }

    @Override
    public @NotNull List<String> getDisabledAggregators() {
        return settings.getDisabledAggregators();
    }

    @Override
    public void setDisabledAggregators(@NotNull List<String> aggregators) {
        settings.setDisabledAggregators(aggregators);
    }

    @Override
    public String getWidgetAggregator() {
        return settings.getWidgetAggregator();
    }

    @Override
    public void setWidgetAggregator(String aggregator) {
        settings.setWidgetAggregator(aggregator);
    }

    @Override
    public boolean isNumberGroupingEnabled() {
        return settings.isNumberGroupingEnabled();
    }

    @Override
    public char getNumberGroupingSeparator() {
        return settings.getNumberGroupingSeparator();
    }

    @Override
    public char getDecimalSeparator() {
        return settings.getDecimalSeparator();
    }

    @Override
    public @NotNull String getInfinity() {
        return settings.getInfinity();
    }

    @Override
    public @NotNull String getNan() {
        return settings.getNan();
    }

    @Override
    public @Nullable String getEffectiveNumberPattern() {
        return settings.getEffectiveNumberPattern();
    }

    @Override
    public @Nullable String getEffectiveDateTimePattern() {
        return settings.getEffectiveDateTimePattern();
    }

    @Override
    public @Nullable String getEffectiveZonedDateTimePattern() {
        return settings.getEffectiveZonedDateTimePattern();
    }

    @Override
    public @Nullable String getEffectiveTimePattern() {
        return settings.getEffectiveTimePattern();
    }

    @Override
    public @Nullable String getEffectiveZonedTimePattern() {
        return settings.getEffectiveZonedTimePattern();
    }

    @Override
    public @Nullable String getEffectiveDatePattern() {
        return settings.getEffectiveDatePattern();
    }

    @Override
    public @Nullable ZoneId getEffectiveZoneId() {
        return settings.getEffectiveZoneId();
    }

    @Override
    public void fireChanged() {
        settings.fireChanged();
    }

    @Override
    public void setPageSize(int value) {
        settings.setPageSize(value);
    }

    @Override
    public int getPageSize() {
        return settings.getPageSize();
    }

    @Override
    public boolean isLimitPageSize() {
        return settings.isLimitPageSize();
    }

    @Override
    public @NotNull ModificationTracker getModificationTracker() {
        return settings.getModificationTracker();
    }

    @Override
    public boolean isOpeningOfHttpsLinksAllowed() {
        return settings.isOpeningOfHttpsLinksAllowed();
    }

    @Override
    public void setIsOpeningOfHttpsLinksAllowed(boolean value) {
        settings.setIsOpeningOfHttpsLinksAllowed(value);
    }

    @Override
    public boolean isOpeningOfHttpLinksAllowed() {
        return settings.isOpeningOfHttpLinksAllowed();
    }

    @Override
    public void setIsOpeningOfHttpLinksAllowed(boolean value) {
        settings.setIsOpeningOfHttpLinksAllowed(value);
    }

    @Override
    public boolean isOpeningOfLocalFileUrlsAllowed() {
        return settings.isOpeningOfLocalFileUrlsAllowed();
    }

    @Override
    public void setIsOpeningOfLocalFileUrlsAllowed(boolean value) {
        settings.setIsOpeningOfLocalFileUrlsAllowed(value);
    }

    @Override
    public boolean isWebUrlWithoutProtocolAssumedHttp() {
        return settings.isWebUrlWithoutProtocolAssumedHttp();
    }

    @Override
    public void setIsWebUrlWithoutProtocolAssumedHttp(boolean value) {
        settings.setIsWebUrlWithoutProtocolAssumedHttp(value);
    }

    @Override
    public boolean isFloatingToolbarCustomizable() {
        return settings.isFloatingToolbarCustomizable();
    }

    @Override
    public void setFloatingToolbarCustomizable(boolean value) {
        settings.setFloatingToolbarCustomizable(value);
    }
}
