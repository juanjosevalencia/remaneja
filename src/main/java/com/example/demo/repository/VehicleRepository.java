package com.example.demo.repository;

import com.example.demo.model.Vehicle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findByOwnerId(Integer ownerId);
}