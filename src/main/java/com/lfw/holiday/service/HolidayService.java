// src/main/java/com/example/holiday/service/HolidayService.java
package com.lfw.holiday.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfw.holiday.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
    private static final String API_URL = "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json";

    // 法定节假日放假天数，与实际放假天数不一样
    private static final Map<String, Integer> LEGAL_HOLIDAY_DAYS = Map.of(
            "元旦", 1,
            "春节", 3,
            "清明节", 1,
            "劳动节", 1,
            "端午节", 1,
            "中秋节", 1,
            "国庆节", 3
    );

    // 近10年春节初一日期记录（手动维护）
    private static final Map<Integer, String> SPRING_FESTIVAL_FIRST_DAY = Map.ofEntries(
            Map.entry(2020, "2020-01-25"),
            Map.entry(2021, "2021-02-12"),
            Map.entry(2022, "2022-02-01"),
            Map.entry(2023, "2023-01-22"),
            Map.entry(2024, "2024-02-10"),
            Map.entry(2025, "2025-01-29"),
            Map.entry(2026, "2026-02-17"),
            Map.entry(2027, "2027-02-06"),
            Map.entry(2028, "2028-01-26"),
            Map.entry(2029, "2029-02-13"),
            Map.entry(2030, "2030-02-03"),
            Map.entry(2031, "2031-01-23"),
            Map.entry(2032, "2032-02-11"),
            Map.entry(2033, "2033-01-31"),
            Map.entry(2034, "2034-02-19"),
            Map.entry(2035, "2035-02-08")
    );

    private final Map<Integer, Map<String, OutputHolidayInfo>> cache = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, OutputHolidayInfo> getHolidayData(int year) {
        return cache.computeIfAbsent(year, y -> fetchFromRemote(y));
    }

    // 公开预热方法（供定时任务调用）
    public void preloadYear(int year) {
        log.info("Preloading holiday data for year {}", year);
        try {
            Map<String, OutputHolidayInfo> data = getHolidayData(year);
            cache.put(year, data);
            log.info("Successfully preloaded holiday data for year {}", year);
        } catch (Exception e) {
            log.error("Failed to preload holiday data for year {}", year, e);
        }
    }

    private Map<String, OutputHolidayInfo> fetchFromRemote(int year) {
        try {
            String url = String.format(API_URL, year);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "HolidayService/1.0 (+https://your-domain.com)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            HolidayApiResponse apiResponse = mapper.readValue(response.body(), HolidayApiResponse.class);
            return convertToMmDdMap(apiResponse);
        } catch (Exception e) {
            log.error("Failed to fetch holiday data for year {}", year, e);
            throw new RuntimeException("Failed to fetch holiday data for year " + year, e);
        }
    }

    /*public Map<String, OutputHolidayInfo> getHolidayData(int year) {
        try {
            String url = String.format(API_URL, year);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
            }

            HolidayApiResponse apiResponse = mapper.readValue(response.body(), HolidayApiResponse.class);
            return convertToMmDdMap(apiResponse);
        } catch (Exception e) {
            log.error("Failed to generate holiday data for year {}", year, e);
            throw new RuntimeException("Failed to generate holiday data for year " + year, e);
        }
    }*/

    private String beautifyName(String originalName, String date, LocalDate springFestivalFirst) {
        if (!"春节".equals(originalName) || springFestivalFirst == null) {
            return originalName; // 其他节日保持原名
        }

        LocalDate currentDate = LocalDate.parse(date);
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(springFestivalFirst, currentDate);

        return switch ((int) daysDiff) {
            case -1 -> "除夕";
            case 0 -> "初一";
            case 1 -> "初二";
            case 2 -> "初三";
            case 3 -> "初四";
            case 4 -> "初五";
            case 5 -> "初六";
            case 6 -> "初七";
            default -> "春节"; // fallback
        };
    }

    private Map<String, OutputHolidayInfo> convertToMmDdMap(HolidayApiResponse response) {
        Set<String> legalHolidays = getLegalHolidayDates(response.days);
        Map<String, LocalDateRange> festivalRanges = buildFestivalRanges(response.days);

        LocalDate springFestivalFirst = Optional.ofNullable(SPRING_FESTIVAL_FIRST_DAY.get(response.year))
                .map(LocalDate::parse)
                .orElse(null);

        return response.days.stream().collect(Collectors.toMap(
                day -> day.date.substring(5), // "2025-01-29" → "01-29"
                day -> {
                    if (day.isOffDay) {
                        // ✅ 仅放假日期美化名称
                        String displayName = beautifyName(day.name, day.date, springFestivalFirst);
                        int wage = legalHolidays.contains(day.date) ? 3 : 2;
                        return new OutputHolidayInfo(true, displayName, wage, day.date);
                    } else {
                        // ❌ 补班日：使用原始节日名（不美化）
                        String target = day.name;
                        LocalDate workDate = LocalDate.parse(day.date);
                        LocalDateRange range = festivalRanges.get(target);
                        boolean after = range != null && workDate.isAfter(range.end);
                        String name = target + (after ? "后" : "前") + "补班";

                        OutputHolidayInfo info = new OutputHolidayInfo(false, name, 1, day.date);
                        info.after = after;
                        info.target = target;
                        return info;
                    }
                },
                (existing, replacement) -> existing, // 理论上不会冲突
                LinkedHashMap::new // 保持原始顺序
        ));
    }

    /**
     * 获取每个节日的法定假日日期（按时间顺序取前 N 天）
     */
    private Set<String> getLegalHolidayDates(List<HolidayDay> days) {
        Map<String, List<String>> festivalDates = new HashMap<>();
        for (HolidayDay day : days) {
            if (day.isOffDay) {
                festivalDates.computeIfAbsent(day.name, k -> new ArrayList<>()).add(day.date);
            }
        }

        Set<String> legalDates = new HashSet<>();
        for (Map.Entry<String, List<String>> entry : festivalDates.entrySet()) {
            String name = entry.getKey();
            List<String> sortedDates = entry.getValue().stream().sorted().toList();
            int legalCount = LEGAL_HOLIDAY_DAYS.getOrDefault(name, 1);
            for (int i = 0; i < Math.min(legalCount, sortedDates.size()); i++) {
                legalDates.add(sortedDates.get(i));
            }
        }
        return legalDates;
    }

    /**
     * 构建每个节日的放假范围 [start, end]
     */
    private Map<String, LocalDateRange> buildFestivalRanges(List<HolidayDay> days) {
        Map<String, List<LocalDate>> map = new HashMap<>();
        for (HolidayDay d : days) {
            if (d.isOffDay) {
                map.computeIfAbsent(d.name, k -> new ArrayList<>())
                        .add(LocalDate.parse(d.date));
            }
        }

        Map<String, LocalDateRange> ranges = new HashMap<>();
        for (Map.Entry<String, List<LocalDate>> e : map.entrySet()) {
            List<LocalDate> list = e.getValue();
            LocalDate start = Collections.min(list);
            LocalDate end = Collections.max(list);
            ranges.put(e.getKey(), new LocalDateRange(start, end));
        }
        return ranges;
    }

    // 辅助类：表示一个日期区间
    public static class LocalDateRange {
        public final LocalDate start;
        public final LocalDate end;

        public LocalDateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}

