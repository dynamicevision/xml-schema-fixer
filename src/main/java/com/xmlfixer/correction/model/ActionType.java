package com.xmlfixer.correction.model;

public enum ActionType {
    ADD_ELEMENT("Add element"),
    REMOVE_ELEMENT("Remove element"),
    MOVE_ELEMENT("Move element"),
    MODIFY_ELEMENT("Modify element"),
    ADD_ATTRIBUTE("Add attribute"),
    REMOVE_ATTRIBUTE("Remove attribute"),
    MODIFY_ATTRIBUTE("Modify attribute"),
    CHANGE_TEXT_CONTENT("Change text content"),
    REORDER_ELEMENTS("Reorder elements"),
    FIX_NAMESPACE("Fix namespace");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
