package com.yuqing.magic.mybatis.bean;

/**
 * Sql改写
 * @author yuqing
 * @date 2018-01-13
 */
public class SqlModifier {

    public static final String HEADER = "__header__";

    public static final String TAILING = "__tailing__";

    private String column;

    private String modification;

    /**
     *
     * @param column 需要改写的列，特殊的列有HEADER，TAILING
     * @param modification
     */
    public SqlModifier(String column, String modification) {
        this.column = column;
        this.modification = modification;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getModification() {
        return modification;
    }

    public void setModification(String modification) {
        this.modification = modification;
    }
}
