package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchaseagreement")
public class PurchaseAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "vehicleId", nullable = false)
    private Integer vehicleId;

    @Column(name = "buyerId", nullable = false)
    private Integer buyerId;

    @Column(name = "sellerId", nullable = false)
    private Integer sellerId;

    @Column(name = "agreementStatus", length = 50)
    private String agreementStatus;

    @Column(name = "agreedAmount", precision = 12, scale = 2)
    private BigDecimal agreedAmount;

    @Column(name = "agreementDate")
    private LocalDateTime agreementDate;

    public PurchaseAgreement() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public String getAgreementStatus() { return agreementStatus; }
    public void setAgreementStatus(String agreementStatus) { this.agreementStatus = agreementStatus; }

    public BigDecimal getAgreedAmount() { return agreedAmount; }
    public void setAgreedAmount(BigDecimal agreedAmount) { this.agreedAmount = agreedAmount; }

    public LocalDateTime getAgreementDate() { return agreementDate; }
    public void setAgreementDate(LocalDateTime agreementDate) { this.agreementDate = agreementDate; }
}