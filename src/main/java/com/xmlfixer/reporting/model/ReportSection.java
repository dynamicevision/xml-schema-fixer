package com.xmlfixer.reporting.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a section within a processing report
 */
public class ReportSection {

    public enum SectionType {
        SUMMARY,
        VALIDATION_DETAILS,
        CORRECTION_DETAILS,
        ERROR_LIST,
        WARNING_LIST,
        STATISTICS,
        RECOMMENDATIONS
    }

    private String title;
    private SectionType type;
    private String content;
    private List<String> items;
    private int order;

    public ReportSection() {
        this.items = new ArrayList<>();
        this.order = 0;
    }

    public ReportSection(String title, SectionType type) {
        this();
        this.title = title;
        this.type = type;
    }

    public ReportSection(String title, SectionType type, String content) {
        this(title, type);
        this.content = content;
    }

    // Utility methods
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }

    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public String toString() {
        return String.format("ReportSection{title='%s', type=%s, hasContent=%s, itemCount=%d}",
            title, type, hasContent(), getItemCount());
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public SectionType getType() { return type; }
    public void setType(SectionType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    // Items list
    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }

    public void addItem(String item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }

    public void addItems(List<String> items) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.addAll(items);
    }
}
