package com.lfw.holiday.scheduler;

import com.lfw.holiday.service.HolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class HolidayCacheScheduler {

    private static final Logger log = LoggerFactory.getLogger(HolidayCacheScheduler.class);
    private final HolidayService holidayService;

    public HolidayCacheScheduler(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * 每年 12 月 1 日凌晨 2 点预加载下一年数据
     */
    @Scheduled(cron = "0 0 2 1 12 *")
    public void preloadNextYearHoliday() {
        int nextYear = LocalDate.now().getYear() + 1;
        log.info("Scheduled task: Preloading holiday data for next year: {}", nextYear);
        holidayService.preloadYear(nextYear);
    }

    /**
     * 应用启动时预加载当前年 + 下一年（仅执行一次）
     */
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 5000) // 延迟 5 秒后执行
    public void preloadOnStartup() {
        int current = LocalDate.now().getYear();
        log.info("Loading holiday data for current year: {}", current);
        holidayService.preloadYear(current);
        holidayService.preloadYear(current + 1);
    }
}