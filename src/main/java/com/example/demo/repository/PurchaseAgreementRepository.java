package com.example.demo.repository;

import com.example.demo.model.PurchaseAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseAgreementRepository extends JpaRepository<PurchaseAgreement, Integer> {
    List<PurchaseAgreement> findByBuyerId(Integer buyerId);
    List<PurchaseAgreement> findBySellerId(Integer sellerId);
    List<PurchaseAgreement> findByVehicleId(Integer vehicleId);
}