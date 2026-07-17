package com.nvlad.yii2support.common;

import org.jetbrains.annotations.NotNull;

/**
 * Created by oleg on 06.04.2017.
 */
public class VirtualProperty {
    private String name;
    private String type;
    private String comment;
    private String prevColumn;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPrevColumn() {
        return prevColumn;
    }

    public void setPrevColumn(String prevColumn) {
        this.prevColumn = prevColumn;
    }

    public VirtualProperty(String name, String typeName, String typeFull, String comment, String prevColumn) {
        this.name = name;
        this.type = DbTypeToSql(typeName);

        this.comment = comment != null ? comment : "";
        if (! typeName.contains("text")) {
            this.comment = "[" +  typeFull + "]  " + this.comment;
        }

        this.prevColumn = prevColumn;
    }

    @NotNull
    private String DbTypeToSql(String type) {
        return switch (type.toUpperCase()) {
            case "INT", "BIGINT", "SMALLINT", "NUMERIC", "MEDIUMINT",
                    "SMALLSERIAL", "SERIAL", "BIGSERIAL", "TIMESTAMP" -> "int";
            case "TINYINT", "BOOLEAN" -> "bool";
            case "FLOAT", "REAL", "DOUBLE PRECISION", "DOUBLE" -> "float";
            default -> "string";
        };
    }
}
