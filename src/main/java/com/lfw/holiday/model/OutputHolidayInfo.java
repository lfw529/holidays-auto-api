package com.lfw.holiday.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 输出结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutputHolidayInfo {
    public boolean holiday;
    public String name;
    public int wage;
    public String date;
    public Boolean after;     // 仅补班日有
    public String target;     // 仅补班日有

    public OutputHolidayInfo(boolean holiday, String name, int wage, String date) {
        this.holiday = holiday;
        this.name = name;
        this.wage = wage;
        this.date = date;
    }
}
