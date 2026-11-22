package com.example.demo.control;

import com.example.demo.model.PurchaseAgreement;
import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.example.demo.service.PurchaseAgreementService;
import com.example.demo.service.UserService;
import com.example.demo.service.VehicleService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebViewControl {

    private final UserService userService;
    private final VehicleService vehicleService;
    private final PurchaseAgreementService purchaseAgreementService;

    public WebViewControl(
            UserService userService,
            VehicleService vehicleService,
            PurchaseAgreementService purchaseAgreementService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.purchaseAgreementService = purchaseAgreementService;
    }

    // ==================== PÁGINA PRINCIPAL ====================
    @GetMapping("/webview")
    public String mostrarPaginaPrincipal(Model model) {
        List<Vehicle> listaVehiculos = vehicleService.listarVehiculos();
        model.addAttribute("listaVehiculos", listaVehiculos);
        model.addAttribute("totalUsuarios", userService.listarUsuarios().size());
        model.addAttribute("totalVehiculos", listaVehiculos.size());
        model.addAttribute("totalAcuerdos", purchaseAgreementService.listarAcuerdos().size());
        return "index";
    }

    // ==================== CRUD USUARIOS ====================
    @GetMapping("/web/users")
    public String mostrarUsuarios(Model model) {
        model.addAttribute("listaUsuarios", userService.listarUsuarios());
        model.addAttribute("usuarioForm", new User());
        model.addAttribute("modoEdicion", false);
        return "users";
    }

    @GetMapping("/web/users/edit/{userId}")
    public String editarUsuario(@PathVariable Integer userId, Model model, RedirectAttributes redirectAttributes) {
        return userService.obtenerUsuarioPorId(userId).map(usuario -> {
            model.addAttribute("listaUsuarios", userService.listarUsuarios());
            model.addAttribute("usuarioForm", usuario);
            model.addAttribute("modoEdicion", true);
            return "users";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el usuario con documento #" + userId);
            return "redirect:/web/users";
        });
    }

    @PostMapping("/web/users/save")
    public String guardarUsuario(
            @ModelAttribute("usuarioForm") User usuarioForm,
            @RequestParam(defaultValue = "false") boolean modoEdicion,
            RedirectAttributes redirectAttributes) {
        if (modoEdicion) {
            userService.actualizarUsuario(usuarioForm.getId(), usuarioForm);
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario #" + usuarioForm.getId() + " actualizado correctamente.");
        } else {
            User usuarioCreado = userService.crearUsuario(usuarioForm);
            if (usuarioCreado == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "No se pudo registrar: el documento es inválido o ya existe.");
                return "redirect:/web/users";
            }
            redirectAttributes.addFlashAttribute("mensajeExito", "Usuario registrado con documento #" + usuarioCreado.getId());
        }
        return "redirect:/web/users";
    }

    @PostMapping("/web/users/delete/{userId}")
    public String eliminarUsuario(@PathVariable Integer userId, RedirectAttributes redirectAttributes) {
        try {
            if (userService.eliminarUsuario(userId)) {
                redirectAttributes.addFlashAttribute("mensajeExito", "Usuario #" + userId + " eliminado del directorio.");
            } else {
                redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el usuario #" + userId);
            }
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se puede eliminar el usuario porque tiene vehículos o acuerdos asociados.");
        }
        return "redirect:/web/users";
    }

    // ==================== CRUD VEHÍCULOS ====================
    @GetMapping("/web/vehicles")
    public String mostrarVehiculos(Model model) {
        model.addAttribute("listaVehiculos", vehicleService.listarVehiculos());
        model.addAttribute("listaUsuarios", userService.listarUsuarios());
        model.addAttribute("vehiculoForm", new Vehicle());
        model.addAttribute("modoEdicion", false);
        return "vehicles";
    }

    @GetMapping("/web/vehicles/edit/{carId}")
    public String editarVehiculo(@PathVariable Integer carId, Model model, RedirectAttributes redirectAttributes) {
        return vehicleService.obtenerVehiculoPorId(carId).map(vehiculo -> {
            model.addAttribute("listaVehiculos", vehicleService.listarVehiculos());
            model.addAttribute("listaUsuarios", userService.listarUsuarios());
            model.addAttribute("vehiculoForm", vehiculo);
            model.addAttribute("modoEdicion", true);
            return "vehicles";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el vehículo #" + carId);
            return "redirect:/web/vehicles";
        });
    }

    @PostMapping("/web/vehicles/save")
    public String guardarVehiculo(
            @ModelAttribute("vehiculoForm") Vehicle vehiculoForm,
            @RequestParam(defaultValue = "false") boolean modoEdicion,
            RedirectAttributes redirectAttributes) {
        if (vehiculoForm.getOwnerId() != null) {
            userService.obtenerUsuarioPorId(vehiculoForm.getOwnerId())
                    .ifPresent(dueno -> vehiculoForm.setOwnerName(dueno.getName()));
        }
        if (modoEdicion) {
            vehicleService.actualizarVehiculo(vehiculoForm.getCarId(), vehiculoForm);
            redirectAttributes.addFlashAttribute("mensajeExito", "Vehículo #" + vehiculoForm.getCarId() + " actualizado.");
        } else {
            Vehicle vehiculoCreado = vehicleService.crearVehiculo(vehiculoForm);
            if (vehiculoCreado == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "Verifica que el ID del vehículo no esté repetido y que el propietario exista.");
                return "redirect:/web/vehicles";
            }
            redirectAttributes.addFlashAttribute("mensajeExito", "Vehículo #" + vehiculoCreado.getCarId() + " publicado en el catálogo.");
        }
        return "redirect:/web/vehicles";
    }

    @PostMapping("/web/vehicles/delete/{carId}")
    public String eliminarVehiculo(@PathVariable Integer carId, RedirectAttributes redirectAttributes) {
        try {
            if (vehicleService.eliminarVehiculo(carId)) {
                redirectAttributes.addFlashAttribute("mensajeExito", "Vehículo #" + carId + " eliminado del inventario.");
            } else {
                redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el vehículo #" + carId);
            }
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se puede eliminar el vehículo porque está vinculado a un acuerdo de compra.");
        }
        return "redirect:/web/vehicles";
    }

    // ==================== CRUD ACUERDOS DE COMPRA ====================
    @GetMapping("/web/purchaseagreements")
    public String mostrarAcuerdos(Model model) {
        model.addAttribute("listaAcuerdos", purchaseAgreementService.listarAcuerdos());
        model.addAttribute("listaVehiculos", vehicleService.listarVehiculos());
        model.addAttribute("listaUsuarios", userService.listarUsuarios());
        model.addAttribute("acuerdoForm", new PurchaseAgreement());
        model.addAttribute("modoEdicion", false);
        return "purchaseagreements";
    }

    @GetMapping("/web/purchaseagreements/edit/{agreementId}")
    public String editarAcuerdo(@PathVariable Integer agreementId, Model model, RedirectAttributes redirectAttributes) {
        return purchaseAgreementService.obtenerAcuerdoPorId(agreementId).map(acuerdo -> {
            model.addAttribute("listaAcuerdos", purchaseAgreementService.listarAcuerdos());
            model.addAttribute("listaVehiculos", vehicleService.listarVehiculos());
            model.addAttribute("listaUsuarios", userService.listarUsuarios());
            model.addAttribute("acuerdoForm", acuerdo);
            model.addAttribute("modoEdicion", true);
            return "purchaseagreements";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el acuerdo #" + agreementId);
            return "redirect:/web/purchaseagreements";
        });
    }

    @PostMapping("/web/purchaseagreements/save")
    public String guardarAcuerdo(
            @ModelAttribute("acuerdoForm") PurchaseAgreement acuerdoForm,
            @RequestParam(defaultValue = "false") boolean modoEdicion,
            RedirectAttributes redirectAttributes) {
        if (modoEdicion) {
            purchaseAgreementService.actualizarAcuerdo(acuerdoForm.getId(), acuerdoForm);
            redirectAttributes.addFlashAttribute("mensajeExito", "Acuerdo #" + acuerdoForm.getId() + " actualizado.");
        } else {
            PurchaseAgreement acuerdoCreado = purchaseAgreementService.crearAcuerdo(acuerdoForm);
            if (acuerdoCreado == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "Error al crear acuerdo: verifica que comprador y vendedor sean distintos y que los registros existan.");
                return "redirect:/web/purchaseagreements";
            }
            redirectAttributes.addFlashAttribute("mensajeExito", "Acuerdo #" + acuerdoCreado.getId() + " generado correctamente.");
        }
        return "redirect:/web/purchaseagreements";
    }

    @PostMapping("/web/purchaseagreements/delete/{agreementId}")
    public String eliminarAcuerdo(@PathVariable Integer agreementId, RedirectAttributes redirectAttributes) {
        if (purchaseAgreementService.eliminarAcuerdo(agreementId)) {
            redirectAttributes.addFlashAttribute("mensajeExito", "Acuerdo #" + agreementId + " eliminado.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el acuerdo #" + agreementId);
        }
        return "redirect:/web/purchaseagreements";
    }

    // ==================== INFORMES Y FILTRADO ====================
    @GetMapping("/web/reports")
    public String mostrarInformes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String salesStatus,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        List<Vehicle> todosVehiculos = vehicleService.listarVehiculos();
        List<PurchaseAgreement> todosAcuerdos = purchaseAgreementService.listarAcuerdos();

        List<Vehicle> listaVehiculosFiltrados = todosVehiculos.stream()
                .filter(vehiculo -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String textoBusqueda = keyword.trim().toLowerCase();
                    boolean matchModelo = vehiculo.getModel() != null && vehiculo.getModel().toLowerCase().contains(textoBusqueda);
                    boolean matchPlaca = vehiculo.getPlate() != null && vehiculo.getPlate().toLowerCase().contains(textoBusqueda);
                    boolean matchDueno = vehiculo.getOwnerName() != null && vehiculo.getOwnerName().toLowerCase().contains(textoBusqueda);
                    return matchModelo || matchPlaca || matchDueno;
                })
                .filter(vehiculo -> salesStatus == null || salesStatus.isBlank() || "TODOS".equalsIgnoreCase(salesStatus)
                        || (vehiculo.getSalesStatus() != null && vehiculo.getSalesStatus().equalsIgnoreCase(salesStatus)))
                .filter(vehiculo -> type == null || type.isBlank()
                        || (vehiculo.getType() != null && vehiculo.getType().toLowerCase().contains(type.trim().toLowerCase())))
                .filter(vehiculo -> maxPrice == null || (vehiculo.getPrice() != null && vehiculo.getPrice().compareTo(maxPrice) <= 0))
                .collect(Collectors.toList());

        BigDecimal valorInventarioDisponible = BigDecimal.ZERO;
        long cantidadVendidos = 0;

        for (Vehicle vehiculo : todosVehiculos) {
            String estadoVehiculo = vehiculo.getSalesStatus();
            BigDecimal precioVehiculo = vehiculo.getPrice();

            if ("DISPONIBLE".equalsIgnoreCase(estadoVehiculo) && precioVehiculo != null) {
                valorInventarioDisponible = valorInventarioDisponible.add(precioVehiculo);
            }
            if ("VENDIDO".equalsIgnoreCase(estadoVehiculo)) {
                cantidadVendidos++;
            }
        }

        BigDecimal totalVentasCerradas = BigDecimal.ZERO;
        BigDecimal totalEnNegociacion = BigDecimal.ZERO;

        for (PurchaseAgreement acuerdo : todosAcuerdos) {
            String estadoAcuerdo = acuerdo.getAgreementStatus();
            BigDecimal montoAcordado = acuerdo.getAgreedAmount();

            if (montoAcordado != null) {
                if ("FINALIZADO".equalsIgnoreCase(estadoAcuerdo)) {
                    totalVentasCerradas = totalVentasCerradas.add(montoAcordado);
                } else if ("PENDIENTE".equalsIgnoreCase(estadoAcuerdo) || "EN_PROCESO".equalsIgnoreCase(estadoAcuerdo)) {
                    totalEnNegociacion = totalEnNegociacion.add(montoAcordado);
                }
            }
        }

        model.addAttribute("listaVehiculosFiltrados", listaVehiculosFiltrados);
        model.addAttribute("listaAcuerdos", todosAcuerdos);
        model.addAttribute("valorInventarioDisponible", valorInventarioDisponible);
        model.addAttribute("totalVentasCerradas", totalVentasCerradas);
        model.addAttribute("totalEnNegociacion", totalEnNegociacion);
        model.addAttribute("cantidadVendidos", cantidadVendidos);

        model.addAttribute("keyword", keyword);
        model.addAttribute("salesStatus", salesStatus);
        model.addAttribute("type", type);
        model.addAttribute("maxPrice", maxPrice);

        return "reports";
    }

    // ==================== FRONTEND DE NEGOCIO (SALA DE CIERRE) ====================
    @GetMapping("/web/business")
    public String mostrarFlujoNegocio(Model model) {
        List<Vehicle> todosVehiculos = vehicleService.listarVehiculos();
        List<PurchaseAgreement> todosAcuerdos = purchaseAgreementService.listarAcuerdos();

        List<Vehicle> vehiculosDisponibles = todosVehiculos.stream()
                .filter(vehiculo -> "DISPONIBLE".equalsIgnoreCase(vehiculo.getSalesStatus()))
                .collect(Collectors.toList());

        List<PurchaseAgreement> acuerdosActivos = todosAcuerdos.stream()
                .filter(acuerdo -> "PENDIENTE".equalsIgnoreCase(acuerdo.getAgreementStatus()) || "EN_PROCESO".equalsIgnoreCase(acuerdo.getAgreementStatus()))
                .collect(Collectors.toList());

        List<PurchaseAgreement> acuerdosFinalizados = todosAcuerdos.stream()
                .filter(acuerdo -> "FINALIZADO".equalsIgnoreCase(acuerdo.getAgreementStatus()))
                .collect(Collectors.toList());

        model.addAttribute("vehiculosDisponibles", vehiculosDisponibles);
        model.addAttribute("listaUsuarios", userService.listarUsuarios());
        model.addAttribute("acuerdosActivos", acuerdosActivos);
        model.addAttribute("acuerdosFinalizados", acuerdosFinalizados);

        return "business";
    }

    @PostMapping("/web/business/create")
    public String ejecutarOperacionNegocio(
            @RequestParam Integer vehicleId,
            @RequestParam Integer buyerId,
            @RequestParam(required = false) BigDecimal agreedAmount,
            @RequestParam(defaultValue = "false") boolean cierreInmediato,
            RedirectAttributes redirectAttributes) {

        String mensajeError = purchaseAgreementService.iniciarOperacionComercial(
                vehicleId, buyerId, agreedAmount, cierreInmediato);

        if (mensajeError != null) {
            redirectAttributes.addFlashAttribute("mensajeError", mensajeError);
        } else if (cierreInmediato) {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Venta cerrada. El vehículo pasó a VENDIDO y su propiedad fue transferida al comprador.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Reserva registrada. El vehículo quedó en estado RESERVADO pendiente de cierre.");
        }
        return "redirect:/web/business";
    }

    @PostMapping("/web/business/status/{agreementId}")
    public String actualizarEstadoNegocio(
            @PathVariable Integer agreementId,
            @RequestParam String nuevoEstado,
            RedirectAttributes redirectAttributes) {

        String mensajeError = purchaseAgreementService.cambiarEstadoNegocio(agreementId, nuevoEstado);
        if (mensajeError != null) {
            redirectAttributes.addFlashAttribute("mensajeError", mensajeError);
        } else if ("FINALIZADO".equalsIgnoreCase(nuevoEstado)) {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Acuerdo #" + agreementId + " finalizado. Propiedad transferida y vehículo marcado como VENDIDO.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Acuerdo #" + agreementId + " cancelado. El vehículo volvió a estar DISPONIBLE.");
        }
        return "redirect:/web/business";
    }
}