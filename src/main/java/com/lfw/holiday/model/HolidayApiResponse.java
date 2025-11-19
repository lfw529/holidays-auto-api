package com.lfw.holiday.model;

import java.util.List;

/**
 * 新模型类
 */
public class HolidayApiResponse {
    public String $schema;
    public String $id;
    public int year;
    public List<String> papers;
    public List<HolidayDay> days;
}
