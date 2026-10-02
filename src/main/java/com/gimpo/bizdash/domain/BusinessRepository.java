package com.gimpo.bizdash.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 집계 쿼리의 district / category 파라미터는 "" 이면 전체를 뜻한다.
 * (null 파라미터는 DB마다 타입 추론 문제가 있어 빈 문자열 센티널을 쓴다.)
 */
public interface BusinessRepository extends JpaRepository<Business, Long> {

    List<Business> findByExternalIdIn(Collection<String> externalIds);

    record YearCount(int year, long count) {
    }

    record CategoryCount(String category, long count) {
    }

    record DistrictCount(String district, long count) {
    }

    record DateRange(LocalDate minDate, LocalDate maxDate) {
    }

    /** 폐업까지 걸린 기간 계산용 원본 (업종, 인허가일, 폐업일). */
    record ClosedSpan(String category, LocalDate licensedDate, LocalDate closedDate) {
    }

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$YearCount(year(b.licensedDate), count(b))
            from Business b
            where b.licensedDate is not null
              and (:district = '' or b.district = :district)
              and (:category = '' or b.category = :category)
            group by year(b.licensedDate)
            """)
    List<YearCount> countOpenedByYear(@Param("district") String district, @Param("category") String category);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$YearCount(year(b.closedDate), count(b))
            from Business b
            where b.status = com.gimpo.bizdash.domain.BusinessStatus.CLOSED
              and b.closedDate is not null
              and (:district = '' or b.district = :district)
              and (:category = '' or b.category = :category)
            group by year(b.closedDate)
            """)
    List<YearCount> countClosedByYear(@Param("district") String district, @Param("category") String category);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$ClosedSpan(b.category, b.licensedDate, b.closedDate)
            from Business b
            where b.status = com.gimpo.bizdash.domain.BusinessStatus.CLOSED
              and b.licensedDate is not null
              and b.closedDate is not null
              and b.closedDate >= b.licensedDate
              and (:district = '' or b.district = :district)
            """)
    List<ClosedSpan> findClosedSpans(@Param("district") String district);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$CategoryCount(b.category, count(b))
            from Business b
            where b.licensedDate > :from and b.licensedDate <= :to
              and (:district = '' or b.district = :district)
            group by b.category
            order by count(b) desc, b.category
            """)
    List<CategoryCount> countOpenedByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                              @Param("district") String district);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$CategoryCount(b.category, count(b))
            from Business b
            where b.status = com.gimpo.bizdash.domain.BusinessStatus.CLOSED
              and b.closedDate > :from and b.closedDate <= :to
              and (:district = '' or b.district = :district)
            group by b.category
            order by count(b) desc, b.category
            """)
    List<CategoryCount> countClosedByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                              @Param("district") String district);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$DistrictCount(b.district, count(b))
            from Business b
            where b.licensedDate > :from and b.licensedDate <= :to
              and (:category = '' or b.category = :category)
            group by b.district
            """)
    List<DistrictCount> countOpenedByDistrict(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                              @Param("category") String category);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$DistrictCount(b.district, count(b))
            from Business b
            where b.status = com.gimpo.bizdash.domain.BusinessStatus.CLOSED
              and b.closedDate > :from and b.closedDate <= :to
              and (:category = '' or b.category = :category)
            group by b.district
            """)
    List<DistrictCount> countClosedByDistrict(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                              @Param("category") String category);

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$DateRange(min(b.licensedDate), max(b.licensedDate))
            from Business b
            """)
    DateRange findLicensedDateRange();

    @Query("select max(b.closedDate) from Business b where b.status = com.gimpo.bizdash.domain.BusinessStatus.CLOSED")
    LocalDate findLatestClosedDate();

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$DistrictCount(b.district, count(b))
            from Business b group by b.district order by count(b) desc, b.district
            """)
    List<DistrictCount> countByDistrict();

    @Query("""
            select new com.gimpo.bizdash.domain.BusinessRepository$CategoryCount(b.category, count(b))
            from Business b group by b.category order by count(b) desc, b.category
            """)
    List<CategoryCount> countByCategory();
}
