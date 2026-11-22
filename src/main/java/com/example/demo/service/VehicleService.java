package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public Vehicle crearVehiculo(Vehicle vehicle) {
        if (vehicle.getCarId() == null || vehicleRepository.existsById(vehicle.getCarId())) {
            return null;
        }
        if (vehicle.getOwnerId() == null) {
            return null;
        }
        Optional<User> duenoOpt = userRepository.findById(vehicle.getOwnerId());
        if (duenoOpt.isEmpty()) {
            return null;
        }
        if (vehicle.getOwnerName() == null || vehicle.getOwnerName().isBlank()) {
            vehicle.setOwnerName(duenoOpt.get().getName());
        }
        if (vehicle.getPublicationDate() == null) {
            vehicle.setPublicationDate(LocalDateTime.now());
        }
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> listarVehiculos() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> obtenerVehiculoPorId(Integer carId) {
        return vehicleRepository.findById(carId);
    }

    public List<Vehicle> listarVehiculosPorDueno(Integer ownerId) {
        return vehicleRepository.findByOwnerId(ownerId);
    }

    public Optional<Vehicle> actualizarVehiculo(Integer carId, Vehicle vehicleActualizado) {
        return vehicleRepository.findById(carId).map(vehiculoExistente -> {
            vehiculoExistente.setOwnerId(vehicleActualizado.getOwnerId());
            vehiculoExistente.setOwnerName(vehicleActualizado.getOwnerName());
            vehiculoExistente.setModel(vehicleActualizado.getModel());
            vehiculoExistente.setType(vehicleActualizado.getType());
            vehiculoExistente.setReleaseYear(vehicleActualizado.getReleaseYear());
            vehiculoExistente.setKms(vehicleActualizado.getKms());
            vehiculoExistente.setPrice(vehicleActualizado.getPrice());
            vehiculoExistente.setColor(vehicleActualizado.getColor());
            vehiculoExistente.setPlate(vehicleActualizado.getPlate());
            vehiculoExistente.setSalesStatus(vehicleActualizado.getSalesStatus());
            return vehicleRepository.save(vehiculoExistente);
        });
    }

    public boolean eliminarVehiculo(Integer carId) {
        if (vehicleRepository.existsById(carId)) {
            vehicleRepository.deleteById(carId);
            return true;
        }
        return false;
    }
}