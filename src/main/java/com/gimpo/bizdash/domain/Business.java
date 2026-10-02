package com.gimpo.bizdash.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(name = "business",
        uniqueConstraints = @UniqueConstraint(name = "uk_business_external_id", columnNames = "external_id"),
        indexes = {
                @Index(name = "idx_business_district_category", columnList = "district, category"),
                @Index(name = "idx_business_licensed_date", columnList = "licensed_date"),
                @Index(name = "idx_business_closed_date", columnList = "closed_date")
        })
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원본 데이터의 개방서비스명 + 관리번호. 같은 CSV를 다시 적재해도 중복되지 않게 하는 키. */
    @Column(name = "external_id", nullable = false, length = 300)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BusinessStatus status;

    @Column(name = "licensed_date")
    private LocalDate licensedDate;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Column(length = 300)
    private String address;

    /** 주소에서 추출한 읍·면·동 (추출 실패 시 "미상"). */
    @Column(nullable = false, length = 30)
    private String district;

    protected Business() {
    }

    public Business(String externalId, String name, String category, BusinessStatus status,
                    LocalDate licensedDate, LocalDate closedDate, String address, String district) {
        this.externalId = externalId;
        update(name, category, status, licensedDate, closedDate, address, district);
    }

    public void update(String name, String category, BusinessStatus status,
                       LocalDate licensedDate, LocalDate closedDate, String address, String district) {
        this.name = name;
        this.category = category;
        this.status = status;
        this.licensedDate = licensedDate;
        this.closedDate = closedDate;
        this.address = address;
        this.district = district;
    }

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BusinessStatus getStatus() { return status; }
    public LocalDate getLicensedDate() { return licensedDate; }
    public LocalDate getClosedDate() { return closedDate; }
    public String getAddress() { return address; }
    public String getDistrict() { return district; }
}
