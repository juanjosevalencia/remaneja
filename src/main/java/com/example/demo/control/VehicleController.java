package com.example.demo.control;

import com.example.demo.model.Vehicle;
import com.example.demo.service.VehicleService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<Vehicle> registrarVehiculo(@RequestBody Vehicle vehicle) {
        Vehicle nuevoVehiculo = vehicleService.crearVehiculo(vehicle);
        if (nuevoVehiculo == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(nuevoVehiculo, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> listarVehiculos() {
        return ResponseEntity.ok(vehicleService.listarVehiculos());
    }

    @GetMapping("/{carId}")
    public ResponseEntity<Vehicle> obtenerVehiculoPorId(@PathVariable Integer carId) {
        return vehicleService.obtenerVehiculoPorId(carId)
                .map(vehiculoEncontrado -> ResponseEntity.ok(vehiculoEncontrado))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Vehicle>> listarVehiculosPorDueno(@PathVariable Integer ownerId) {
        return ResponseEntity.ok(vehicleService.listarVehiculosPorDueno(ownerId));
    }

    @PutMapping("/{carId}")
    public ResponseEntity<Vehicle> actualizarVehiculo(@PathVariable Integer carId, @RequestBody Vehicle vehicleActualizado) {
        return vehicleService.actualizarVehiculo(carId, vehicleActualizado)
                .map(vehiculoGuardado -> ResponseEntity.ok(vehiculoGuardado))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Integer carId) {
        if (vehicleService.eliminarVehiculo(carId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}