// src/main/java/com/example/holiday/controller/HolidayController.java
package com.lfw.holiday.controller;

import com.lfw.holiday.model.HolidayResponse;
import com.lfw.holiday.model.OutputHolidayInfo;
import com.lfw.holiday.service.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
// 含 Swagger 注解
@Tag(name = "节假日API", description = "获取中国节假日安排")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /*@GetMapping("/holiday")
    public Map<String, OutputHolidayInfo> getHoliday(@RequestParam(defaultValue = "2024") int year) {
        return holidayService.getHolidayData(year);
    }*/

    @Operation(summary = "获取某年节假日数据", description = "返回符合标准格式的节假日 JSON")
    @GetMapping("/holiday")
    public HolidayResponse getHoliday(
            @Parameter(description = "年份，默认为 2024", example = "2024")
            @RequestParam(defaultValue = "2024") int year) {
        Map<String, OutputHolidayInfo> data = holidayService.getHolidayData(year);
        HolidayResponse resp = new HolidayResponse();
        resp.holiday = data;
        return resp;
    }
}