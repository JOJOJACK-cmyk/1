package com.gimpo.bizdash.stats;

import com.gimpo.bizdash.domain.BusinessRepository;
import com.gimpo.bizdash.domain.BusinessRepository.CategoryCount;
import com.gimpo.bizdash.domain.BusinessRepository.ClosedSpan;
import com.gimpo.bizdash.domain.BusinessRepository.DateRange;
import com.gimpo.bizdash.domain.BusinessRepository.DistrictCount;
import com.gimpo.bizdash.domain.BusinessRepository.YearCount;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모든 지표는 "공공데이터에 기록된 값 기준 추정치"다. 폐업 신고 누락·지연이 있을 수 있다.
 * district / category 인자가 빈 문자열이면 전체를 뜻한다.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final double DAYS_PER_YEAR = 365.25;

    public record Meta(long total, LocalDate asOf, Integer minYear, Integer maxYear,
                       List<DistrictCount> districts, List<CategoryCount> categories) {
    }

    public record TrendPoint(int year, long opened, long closed, long net) {
    }

    public record Trend(LocalDate asOf, List<TrendPoint> points) {
    }

    public record SurvivalRow(String category, long closedCount, double avgYears, double medianYears) {
    }

    /** openedTotal / closedTotal 은 TOP 으로 자르기 전, 해당 기간 전체 업종의 합계. */
    public record Ranking(LocalDate asOf, LocalDate from, int months,
                          long openedTotal, long closedTotal,
                          List<CategoryCount> opened, List<CategoryCount> closed) {
    }

    public record DistrictRow(String district, long opened, long closed, long net) {
    }

    public record Districts(LocalDate asOf, LocalDate from, int months, List<DistrictRow> rows) {
    }

    private final BusinessRepository repository;

    public StatsService(BusinessRepository repository) {
        this.repository = repository;
    }

    public Meta meta() {
        DateRange range = repository.findLicensedDateRange();
        Integer minYear = range == null || range.minDate() == null ? null : range.minDate().getYear();
        LocalDate asOf = asOf();
        Integer maxYear = asOf == null ? null : asOf.getYear();
        return new Meta(repository.count(), asOf, minYear, maxYear,
                repository.countByDistrict(), repository.countByCategory());
    }

    /** 연도별 개업·폐업·순증감. 최근 {@code years}년을 보여주고, 데이터가 없는 해는 0으로 채운다. */
    public Trend trend(String district, String category, int years) {
        LocalDate asOf = asOf();
        if (asOf == null) {
            return new Trend(null, List.of());
        }
        int lastYear = asOf.getYear();
        int firstYear = lastYear - years + 1;

        Map<Integer, Long> opened = toMap(repository.countOpenedByYear(district, category));
        Map<Integer, Long> closed = toMap(repository.countClosedByYear(district, category));

        List<TrendPoint> points = new ArrayList<>();
        for (int y = firstYear; y <= lastYear; y++) {
            long o = opened.getOrDefault(y, 0L);
            long c = closed.getOrDefault(y, 0L);
            points.add(new TrendPoint(y, o, c, o - c));
        }
        return new Trend(asOf, points);
    }

    /**
     * 업종별 폐업까지의 영업 기간(인허가일→폐업일). 폐업한 곳만 대상이라 "아직 영업 중인 곳"은 빠진다.
     * 따라서 실제 평균 수명보다 짧게 나오는 편향이 있고, 업종끼리 상대 비교용으로 읽어야 한다.
     */
    public List<SurvivalRow> survival(String district, int minSample, int limit) {
        Map<String, List<Double>> yearsByCategory = new HashMap<>();
        for (ClosedSpan span : repository.findClosedSpans(district)) {
            double years = ChronoUnit.DAYS.between(span.licensedDate(), span.closedDate()) / DAYS_PER_YEAR;
            yearsByCategory.computeIfAbsent(span.category(), k -> new ArrayList<>()).add(years);
        }
        return yearsByCategory.entrySet().stream()
                .filter(e -> e.getValue().size() >= minSample)
                .map(e -> toSurvivalRow(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(SurvivalRow::closedCount).reversed()
                        .thenComparing(SurvivalRow::category))
                .limit(limit)
                .toList();
    }

    /** 최근 {@code months}개월 개업·폐업 업종 랭킹. 기준일은 오늘이 아니라 데이터의 마지막 날짜(asOf). */
    public Ranking ranking(String district, int months, int limit) {
        LocalDate asOf = asOf();
        if (asOf == null) {
            return new Ranking(null, null, months, 0, 0, List.of(), List.of());
        }
        LocalDate from = asOf.minusMonths(months);
        List<CategoryCount> opened = repository.countOpenedByCategory(from, asOf, district);
        List<CategoryCount> closed = repository.countClosedByCategory(from, asOf, district);
        return new Ranking(asOf, from, months, sum(opened), sum(closed), top(opened, limit), top(closed, limit));
    }

    /** 최근 {@code months}개월 읍·면·동별 개업·폐업 (신도시 vs 구도심 비교용). */
    public Districts districts(String category, int months) {
        LocalDate asOf = asOf();
        if (asOf == null) {
            return new Districts(null, null, months, List.of());
        }
        LocalDate from = asOf.minusMonths(months);
        Map<String, Long> opened = toDistrictMap(repository.countOpenedByDistrict(from, asOf, category));
        Map<String, Long> closed = toDistrictMap(repository.countClosedByDistrict(from, asOf, category));

        TreeSet<String> names = new TreeSet<>(opened.keySet());
        names.addAll(closed.keySet());
        List<DistrictRow> rows = names.stream()
                .map(n -> {
                    long o = opened.getOrDefault(n, 0L);
                    long c = closed.getOrDefault(n, 0L);
                    return new DistrictRow(n, o, c, o - c);
                })
                .sorted(Comparator.comparingLong((DistrictRow r) -> r.opened() + r.closed()).reversed()
                        .thenComparing(DistrictRow::district))
                .toList();
        return new Districts(asOf, from, months, rows);
    }

    /** 데이터에 기록된 가장 최근 날짜. 미래 날짜 오타는 오늘로 자른다. */
    private LocalDate asOf() {
        DateRange range = repository.findLicensedDateRange();
        LocalDate latestClosed = repository.findLatestClosedDate();
        LocalDate latest = null;
        if (range != null && range.maxDate() != null) {
            latest = range.maxDate();
        }
        if (latestClosed != null && (latest == null || latestClosed.isAfter(latest))) {
            latest = latestClosed;
        }
        LocalDate today = LocalDate.now();
        return latest == null ? null : (latest.isAfter(today) ? today : latest);
    }

    private static SurvivalRow toSurvivalRow(String category, List<Double> years) {
        List<Double> sorted = years.stream().sorted().toList();
        double avg = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int n = sorted.size();
        double median = n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
        return new SurvivalRow(category, n, round1(avg), round1(median));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static long sum(List<CategoryCount> counts) {
        return counts.stream().mapToLong(CategoryCount::count).sum();
    }

    private static List<CategoryCount> top(List<CategoryCount> counts, int limit) {
        return counts.stream().limit(limit).toList();
    }

    private static Map<Integer, Long> toMap(List<YearCount> counts) {
        Map<Integer, Long> map = new HashMap<>();
        counts.forEach(c -> map.put(c.year(), c.count()));
        return map;
    }

    private static Map<String, Long> toDistrictMap(List<DistrictCount> counts) {
        Map<String, Long> map = new HashMap<>();
        counts.forEach(c -> map.put(c.district(), c.count()));
        return map;
    }
}
