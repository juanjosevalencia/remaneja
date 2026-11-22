package com.example.demo.service;

import com.example.demo.model.PurchaseAgreement;
import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.example.demo.repository.PurchaseAgreementRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseAgreementService {

    private final PurchaseAgreementRepository purchaseAgreementRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public PurchaseAgreementService(
            PurchaseAgreementRepository purchaseAgreementRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository) {
        this.purchaseAgreementRepository = purchaseAgreementRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public PurchaseAgreement crearAcuerdo(PurchaseAgreement agreement) {
        if (agreement.getVehicleId() == null || !vehicleRepository.existsById(agreement.getVehicleId())) {
            return null;
        }
        if (agreement.getBuyerId() == null || !userRepository.existsById(agreement.getBuyerId())) {
            return null;
        }
        if (agreement.getSellerId() == null || !userRepository.existsById(agreement.getSellerId())) {
            return null;
        }
        if (agreement.getBuyerId().equals(agreement.getSellerId())) {
            return null;
        }
        agreement.setId(null);
        if (agreement.getAgreementDate() == null) {
            agreement.setAgreementDate(LocalDateTime.now());
        }
        return purchaseAgreementRepository.save(agreement);
    }

    public List<PurchaseAgreement> listarAcuerdos() {
        return purchaseAgreementRepository.findAll();
    }

    public Optional<PurchaseAgreement> obtenerAcuerdoPorId(Integer agreementId) {
        return purchaseAgreementRepository.findById(agreementId);
    }

    public List<PurchaseAgreement> listarAcuerdosPorComprador(Integer buyerId) {
        return purchaseAgreementRepository.findByBuyerId(buyerId);
    }

    public List<PurchaseAgreement> listarAcuerdosPorVendedor(Integer sellerId) {
        return purchaseAgreementRepository.findBySellerId(sellerId);
    }

    public Optional<PurchaseAgreement> actualizarAcuerdo(Integer agreementId, PurchaseAgreement agreementActualizado) {
        return purchaseAgreementRepository.findById(agreementId).map(acuerdoExistente -> {
            acuerdoExistente.setVehicleId(agreementActualizado.getVehicleId());
            acuerdoExistente.setBuyerId(agreementActualizado.getBuyerId());
            acuerdoExistente.setSellerId(agreementActualizado.getSellerId());
            acuerdoExistente.setAgreementStatus(agreementActualizado.getAgreementStatus());
            acuerdoExistente.setAgreedAmount(agreementActualizado.getAgreedAmount());
            return purchaseAgreementRepository.save(acuerdoExistente);
        });
    }

    public boolean eliminarAcuerdo(Integer agreementId) {
        if (purchaseAgreementRepository.existsById(agreementId)) {
            purchaseAgreementRepository.deleteById(agreementId);
            return true;
        }
        return false;
    }

    // ==================== LÓGICA DE NEGOCIO ====================
    @Transactional
    public String iniciarOperacionComercial(
            Integer vehicleId,
            Integer buyerId,
            BigDecimal agreedAmount,
            boolean cierreInmediato) {

        Optional<Vehicle> vehiculoOpt = vehicleRepository.findById(vehicleId);
        if (vehiculoOpt.isEmpty()) {
            return "El vehículo seleccionado no existe.";
        }

        Vehicle vehiculo = vehiculoOpt.get();
        if ("VENDIDO".equalsIgnoreCase(vehiculo.getSalesStatus())) {
            return "Este vehículo ya figura como VENDIDO.";
        }

        Optional<User> compradorOpt = userRepository.findById(buyerId);
        if (compradorOpt.isEmpty()) {
            return "El comprador seleccionado no existe.";
        }

        User comprador = compradorOpt.get();
        if (comprador.getId().equals(vehiculo.getOwnerId())) {
            return "El comprador no puede ser el mismo propietario actual del vehículo.";
        }

        PurchaseAgreement nuevoAcuerdo = new PurchaseAgreement();
        nuevoAcuerdo.setVehicleId(vehiculo.getCarId());
        nuevoAcuerdo.setSellerId(vehiculo.getOwnerId());
        nuevoAcuerdo.setBuyerId(comprador.getId());
        nuevoAcuerdo.setAgreedAmount(agreedAmount != null ? agreedAmount : vehiculo.getPrice());
        nuevoAcuerdo.setAgreementDate(LocalDateTime.now());

        if (cierreInmediato) {
            nuevoAcuerdo.setAgreementStatus("FINALIZADO");
            vehiculo.setSalesStatus("VENDIDO");
            vehiculo.setOwnerId(comprador.getId());
            vehiculo.setOwnerName(comprador.getName());
        } else {
            nuevoAcuerdo.setAgreementStatus("PENDIENTE");
            vehiculo.setSalesStatus("RESERVADO");
        }

        vehicleRepository.save(vehiculo);
        purchaseAgreementRepository.save(nuevoAcuerdo);
        return null;
    }

    @Transactional
    public String cambiarEstadoNegocio(Integer agreementId, String nuevoEstado) {
        Optional<PurchaseAgreement> acuerdoOpt = purchaseAgreementRepository.findById(agreementId);
        if (acuerdoOpt.isEmpty()) {
            return "No se encontró el acuerdo #" + agreementId;
        }

        PurchaseAgreement acuerdo = acuerdoOpt.get();
        Optional<Vehicle> vehiculoOpt = vehicleRepository.findById(acuerdo.getVehicleId());
        if (vehiculoOpt.isEmpty()) {
            return "No se encontró el vehículo vinculado al acuerdo.";
        }

        Vehicle vehiculo = vehiculoOpt.get();

        if ("FINALIZADO".equalsIgnoreCase(nuevoEstado)) {
            Optional<User> compradorOpt = userRepository.findById(acuerdo.getBuyerId());
            if (compradorOpt.isEmpty()) {
                return "No se encontró el comprador para transferir la titularidad.";
            }
            User comprador = compradorOpt.get();
            acuerdo.setAgreementStatus("FINALIZADO");
            acuerdo.setAgreementDate(LocalDateTime.now());

            vehiculo.setSalesStatus("VENDIDO");
            vehiculo.setOwnerId(comprador.getId());
            vehiculo.setOwnerName(comprador.getName());
        } else if ("CANCELADO".equalsIgnoreCase(nuevoEstado)) {
            acuerdo.setAgreementStatus("CANCELADO");
            vehiculo.setSalesStatus("DISPONIBLE");
        } else {
            return "Estado de operación no válido.";
        }

        vehicleRepository.save(vehiculo);
        purchaseAgreementRepository.save(acuerdo);
        return null;
    }
}