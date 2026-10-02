package com.gimpo.bizdash.web;

import com.gimpo.bizdash.stats.StatsService;
import com.gimpo.bizdash.stats.StatsService.Districts;
import com.gimpo.bizdash.stats.StatsService.Meta;
import com.gimpo.bizdash.stats.StatsService.Ranking;
import com.gimpo.bizdash.stats.StatsService.SurvivalRow;
import com.gimpo.bizdash.stats.StatsService.Trend;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/meta")
    public Meta meta() {
        return stats.meta();
    }

    /** 연도별 개업·폐업·순증감. district / category 를 비우면 전체. */
    @GetMapping("/trend")
    public Trend trend(@RequestParam(defaultValue = "") String district,
                       @RequestParam(defaultValue = "") String category,
                       @RequestParam(defaultValue = "15") int years) {
        return stats.trend(district, category, clamp(years, 3, 40));
    }

    /** 업종별 영업 기간(폐업한 곳 기준). */
    @GetMapping("/survival")
    public List<SurvivalRow> survival(@RequestParam(defaultValue = "") String district,
                                      @RequestParam(defaultValue = "10") int minSample,
                                      @RequestParam(defaultValue = "12") int limit) {
        return stats.survival(district, Math.max(1, minSample), clamp(limit, 1, 50));
    }

    /** 최근 N개월 개업·폐업 TOP 업종. */
    @GetMapping("/ranking")
    public Ranking ranking(@RequestParam(defaultValue = "") String district,
                           @RequestParam(defaultValue = "12") int months,
                           @RequestParam(defaultValue = "10") int limit) {
        return stats.ranking(district, clamp(months, 1, 60), clamp(limit, 1, 50));
    }

    /** 최근 N개월 읍·면·동별 개업·폐업. */
    @GetMapping("/districts")
    public Districts districts(@RequestParam(defaultValue = "") String category,
                               @RequestParam(defaultValue = "12") int months) {
        return stats.districts(category, clamp(months, 1, 60));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
