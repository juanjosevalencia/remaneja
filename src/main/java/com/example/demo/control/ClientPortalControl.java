package com.example.demo.control;

import com.example.demo.model.PurchaseAgreement;
import com.example.demo.model.User;
import com.example.demo.model.Vehicle;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.VehicleRepository;
import com.example.demo.service.PurchaseAgreementService;
import com.example.demo.service.PurchaseOrderPdfService;
import com.example.demo.service.UserService;
import com.example.demo.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClientPortalControl {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/portal";
    }

    private final UserService userService;
    private final UserRepository userRepository;
    private final VehicleService vehicleService;
    private final VehicleRepository vehicleRepository;
    private final PurchaseAgreementService purchaseAgreementService;
    private final PurchaseOrderPdfService purchaseOrderPdfService;

    public ClientPortalControl(
            UserService userService,
            VehicleService vehicleService,
            PurchaseAgreementService purchaseAgreementService,
            PurchaseOrderPdfService purchaseOrderPdfService,
            UserRepository userRepository,
            VehicleRepository vehicleRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.vehicleService = vehicleService;
        this.purchaseAgreementService = purchaseAgreementService;
        this.purchaseOrderPdfService = purchaseOrderPdfService;
        this.vehicleRepository = vehicleRepository;
    }

    private boolean esAdmin(User usuario) {
        return usuario != null
                && usuario.getRole() != null
                && "ADMIN".equalsIgnoreCase(usuario.getRole().trim());
    }

    // ==================== PORTAL INICIAL / CATÁLOGO CLIENTE ====================
    @GetMapping("/portal")
    public String mostrarPortalCliente(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "catalogo") String tab,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        User clienteActual = (User) session.getAttribute("clienteLogueado");
        if (esAdmin(clienteActual)) {
            redirectAttributes.addFlashAttribute("mensajeError", "Estás autenticado como administrador; estás en el panel de admin.");
            return "redirect:/admin";
        }

        List<Vehicle> todosVehiculos = vehicleService.listarVehiculos();

        List<Vehicle> catalogoDisponible = todosVehiculos.stream()
                .filter(vehiculo -> "DISPONIBLE".equalsIgnoreCase(vehiculo.getSalesStatus()))
                .filter(vehiculo -> clienteActual == null || !clienteActual.getId().equals(vehiculo.getOwnerId()))
                .filter(vehiculo -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String q = keyword.trim().toLowerCase();
                    boolean matchModelo = vehiculo.getModel() != null && vehiculo.getModel().toLowerCase().contains(q);
                    boolean matchPlaca = vehiculo.getPlate() != null && vehiculo.getPlate().toLowerCase().contains(q);
                    boolean matchTipo = vehiculo.getType() != null && vehiculo.getType().toLowerCase().contains(q);
                    return matchModelo || matchPlaca || matchTipo;
                })
                .collect(Collectors.toList());

        model.addAttribute("clienteLogueado", clienteActual);
        model.addAttribute("catalogoDisponible", catalogoDisponible);
        model.addAttribute("vehiculoNuevo", new Vehicle());
        model.addAttribute("keyword", keyword);
        model.addAttribute("tabActiva", tab);

        if (clienteActual != null) {
            Integer clientId = clienteActual.getId();

            List<Vehicle> misVehiculos = todosVehiculos.stream()
                    .filter(vehiculo -> clientId.equals(vehiculo.getOwnerId()))
                    .collect(Collectors.toList());

            List<PurchaseAgreement> misOfertasEnviadas = purchaseAgreementService.listarAcuerdosPorComprador(clientId);
            List<PurchaseAgreement> ofertasRecibidas = purchaseAgreementService.listarAcuerdosPorVendedor(clientId);

            model.addAttribute("misVehiculos", misVehiculos);
            model.addAttribute("misOfertasEnviadas", misOfertasEnviadas);
            model.addAttribute("ofertasRecibidas", ofertasRecibidas);
        }

        return "portal";
    }

    // ==================== PANEL DE ADMINISTRADOR ====================
    @GetMapping("/admin")
    public String mostrarPanelAdmin(
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        User usuarioActual = (User) session.getAttribute("clienteLogueado");
        if (usuarioActual == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Debes iniciar sesión para acceder al panel de administrador.");
            return "redirect:/login";
        }
        if (!esAdmin(usuarioActual)) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permisos de administrador.");
            return "redirect:/portal";
        }

        model.addAttribute("adminActual", usuarioActual);
        model.addAttribute("vehiculos", vehicleService.listarVehiculos());
        model.addAttribute("usuarios", userService.listarUsuarios());
        return "admin";
    }

    // ==================== INICIAR SESIÓN ====================
    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        User usuarioActual = (User) session.getAttribute("clienteLogueado");
        if (usuarioActual != null) {
            return esAdmin(usuarioActual) ? "redirect:/admin" : "redirect:/portal";
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<User> usuarioOpt = userService.autenticarUsuario(email, password);
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "Correo o contraseña incorrectos.");
            return "redirect:/login";
        }

        User usuario = usuarioOpt.get();
        session.setAttribute("clienteLogueado", usuario);

        if (esAdmin(usuario)) {
            redirectAttributes.addFlashAttribute("mensajeExito", "Bienvenido administrador, " + usuario.getName() + ".");
            return "redirect:/admin";
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "Bienvenido de nuevo, " + usuario.getName() + ".");
        return "redirect:/portal";
    }

    // ==================== REGISTRAR CUENTA ====================
    @GetMapping("/register")
    public String mostrarRegistro(Model model, HttpSession session) {
        User usuarioActual = (User) session.getAttribute("clienteLogueado");
        if (usuarioActual != null) {
            return esAdmin(usuarioActual) ? "redirect:/admin" : "redirect:/portal";
        }
        model.addAttribute("nuevoCliente", new User());
        return "register";
    }

    @PostMapping("/register")
    public String procesarRegistro(
            @ModelAttribute("nuevoCliente") User nuevoCliente,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (nuevoCliente.getRole() == null || nuevoCliente.getRole().isBlank()) {
            nuevoCliente.setRole("USER");
        }

        User creado = userService.crearUsuario(nuevoCliente);
        if (creado == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo crear la cuenta: verifica que el documento no esté registrado previamente.");
            return "redirect:/register";
        }

        session.setAttribute("clienteLogueado", creado);
        redirectAttributes.addFlashAttribute("mensajeExito", "Cuenta creada con éxito. Ya puedes publicar o ofertar por vehículos.");
        return "redirect:/portal";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("mensajeExito", "Has cerrado sesión correctamente.");
        return "redirect:/portal";
    }

    // ==================== FUNCIÓN CLIENTE 1: HACER OFERTA DE COMPRA ====================
    @PostMapping("/portal/offer")
    public String enviarOfertaCompra(
            @RequestParam Integer vehicleId,
            @RequestParam BigDecimal agreedAmount,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteActual = (User) session.getAttribute("clienteLogueado");
        if (clienteActual == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Inicia sesión o regístrate para enviar una oferta de compra.");
            return "redirect:/login";
        }
        if (esAdmin(clienteActual)) {
            return "redirect:/admin";
        }

        String mensajeError = purchaseAgreementService.iniciarOperacionComercial(
                vehicleId, clienteActual.getId(), agreedAmount, false);

        if (mensajeError != null) {
            redirectAttributes.addFlashAttribute("mensajeError", mensajeError);
            return "redirect:/portal?tab=catalogo";
        }

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Oferta enviada por el vehículo #" + vehicleId + ". El auto quedó reservado mientras el propietario responde.");
        return "redirect:/portal?tab=procesos";
    }

    // ==================== FUNCIÓN CLIENTE 2: PUBLICAR UN AUTO ====================
    @PostMapping("/portal/publish")
    public String publicarVehiculoCliente(
            @ModelAttribute("vehiculoNuevo") Vehicle vehiculoNuevo,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteActual = (User) session.getAttribute("clienteLogueado");
        if (clienteActual == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Debes iniciar sesión para publicar un vehículo.");
            return "redirect:/login";
        }
        if (esAdmin(clienteActual)) {
            return "redirect:/admin";
        }

        if (imagenFile != null && !imagenFile.isEmpty()) {
            String contentType = imagenFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("mensajeError", "El archivo seleccionado debe ser una imagen válida.");
                return "redirect:/portal?tab=publicar";
            }
            try {
                vehiculoNuevo.setImageData(imagenFile.getBytes());
                vehiculoNuevo.setImageType(contentType);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("mensajeError", "No se pudo leer la imagen del vehículo.");
                return "redirect:/portal?tab=publicar";
            }
        }

        vehiculoNuevo.setOwnerId(clienteActual.getId());
        vehiculoNuevo.setOwnerName(clienteActual.getName());
        vehiculoNuevo.setSalesStatus("DISPONIBLE");

        Vehicle creado = vehicleService.crearVehiculo(vehiculoNuevo);
        if (creado == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se pudo publicar: el ID del vehículo ya existe o faltan datos.");
            return "redirect:/portal?tab=publicar";
        }

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Tu vehículo " + creado.getModel() + " (" + creado.getPlate() + ") ya está visible en el catálogo.");
        return "redirect:/portal?tab=procesos";
    }

    // ==================== VER IMAGEN DEL VEHÍCULO DESDE BD ====================
    @GetMapping("/portal/vehicle/{carId}/image")
    public ResponseEntity<byte[]> obtenerImagenVehiculo(@PathVariable Integer carId) {
        Vehicle vehiculo = vehicleService.listarVehiculos().stream()
                .filter(v -> v.getCarId() != null && v.getCarId().equals(carId))
                .findFirst()
                .orElse(null);

        if (vehiculo == null || vehiculo.getImageData() == null || vehiculo.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        String tipo = (vehiculo.getImageType() != null && !vehiculo.getImageType().isBlank())
                ? vehiculo.getImageType()
                : "image/jpeg";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(tipo));
        headers.setContentLength(vehiculo.getImageData().length);

        return new ResponseEntity<>(vehiculo.getImageData(), headers, HttpStatus.OK);
    }

    // ==================== FUNCIÓN CLIENTE 3: GESTIONAR PROCESO (ACEPTAR / CANCELAR OFERTA) ====================
    @PostMapping("/portal/process/{agreementId}")
    public String responderProcesoVenta(
            @PathVariable Integer agreementId,
            @RequestParam String decision,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteActual = (User) session.getAttribute("clienteLogueado");
        if (clienteActual == null) {
            return "redirect:/login";
        }

        Optional<PurchaseAgreement> acuerdoOpt = purchaseAgreementService.obtenerAcuerdoPorId(agreementId);
        if (acuerdoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el proceso #" + agreementId);
            return "redirect:/portal?tab=procesos";
        }

        PurchaseAgreement acuerdo = acuerdoOpt.get();
        boolean esVendedor = clienteActual.getId().equals(acuerdo.getSellerId());
        boolean esComprador = clienteActual.getId().equals(acuerdo.getBuyerId());

        if (!esVendedor && !esComprador) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permisos sobre este proceso.");
            return "redirect:/portal?tab=procesos";
        }

        if ("FINALIZADO".equalsIgnoreCase(decision) && !esVendedor) {
            redirectAttributes.addFlashAttribute("mensajeError", "Solo el propietario vendedor puede aceptar y transferir el vehículo.");
            return "redirect:/portal?tab=procesos";
        }

        String error = purchaseAgreementService.cambiarEstadoNegocio(agreementId, decision);
        if (error != null) {
            redirectAttributes.addFlashAttribute("mensajeError", error);
        } else if ("FINALIZADO".equalsIgnoreCase(decision)) {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Venta cerrada! La propiedad del vehículo fue transferida al comprador. Ya puedes descargar tu orden de compra en PDF.");
        } else {
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Oferta cancelada. El vehículo volvió a estar disponible en el catálogo.");
        }

        return "redirect:/portal?tab=procesos";
    }

    // ==================== DESCARGAR ORDEN DE COMPRA PDF ====================
    @GetMapping("/portal/order/{agreementId}/pdf")
    public ResponseEntity<byte[]> descargarOrdenCompraPdf(
            @PathVariable Integer agreementId,
            HttpSession session) {

        User clienteActual = (User) session.getAttribute("clienteLogueado");
        if (clienteActual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<PurchaseAgreement> acuerdoOpt = purchaseAgreementService.obtenerAcuerdoPorId(agreementId);
        if (acuerdoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PurchaseAgreement acuerdo = acuerdoOpt.get();
        boolean esParticipante = clienteActual.getId().equals(acuerdo.getBuyerId())
                || clienteActual.getId().equals(acuerdo.getSellerId())
                || esAdmin(clienteActual);
        if (!esParticipante) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Vehicle vehicle = vehicleService.listarVehiculos().stream()
                .filter(v -> v.getCarId() != null && v.getCarId().equals(acuerdo.getVehicleId()))
                .findFirst()
                .orElse(null);

        User buyer = userService.listarUsuarios().stream()
                .filter(u -> u.getId() != null && u.getId().equals(acuerdo.getBuyerId()))
                .findFirst()
                .orElse(null);

        User seller = userService.listarUsuarios().stream()
                .filter(u -> u.getId() != null && u.getId().equals(acuerdo.getSellerId()))
                .findFirst()
                .orElse(null);

        byte[] pdfBytes = purchaseOrderPdfService.generarOrdenCompraPdf(acuerdo, vehicle, buyer, seller);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("Orden-Compra-Remaneja-" + acuerdo.getId() + ".pdf")
                        .build()
        );
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    //MODULO DE ELIMINACIÓN Y EDICIÓN DE VEHICULOS
    @PostMapping("/portal/vehicle/update")
    public String actualizarVehiculoCliente(
            @RequestParam("carId") Integer carId,
            @RequestParam("plate") String plate,
            @RequestParam("model") String model,
            @RequestParam("type") String type,
            @RequestParam("color") String color,
            @RequestParam("releaseYear") Integer releaseYear,
            @RequestParam("kms") Integer kms,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteLogueado = (User) session.getAttribute("clienteLogueado");
        if (clienteLogueado == null) {
            return "redirect:/login";
        }

        Vehicle vehiculo = vehicleRepository.findById(carId).orElse(null);
        if (vehiculo == null || !vehiculo.getOwnerId().equals(clienteLogueado.getId())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para editar este vehículo.");
            return "redirect:/portal?tab=perfil";
        }

        if ("VENDIDO".equalsIgnoreCase(vehiculo.getSalesStatus())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No puedes editar un vehículo que ya fue vendido.");
            return "redirect:/portal?tab=perfil";
        }

        vehiculo.setPlate(plate.trim().toUpperCase());
        vehiculo.setModel(model.trim());
        vehiculo.setType(type.trim());
        vehiculo.setColor(color.trim());
        vehiculo.setReleaseYear(releaseYear);
        vehiculo.setKms(kms);
        vehiculo.setPrice(price);

        try {
            if (imagenFile != null && !imagenFile.isEmpty()) {
                vehiculo.setImageData(imagenFile.getBytes());
                vehiculo.setImageType(imagenFile.getContentType());
            }
            vehicleRepository.save(vehiculo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Información del vehículo actualizada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "Error al actualizar el vehículo: " + e.getMessage());
        }

        return "redirect:/portal?tab=perfil";
    }

    @PostMapping("/portal/vehicle/delete/{carId}")
    public String eliminarVehiculoCliente(
            @PathVariable("carId") Integer carId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteLogueado = (User) session.getAttribute("clienteLogueado");
        if (clienteLogueado == null) {
            return "redirect:/login";
        }

        Vehicle vehiculo = vehicleRepository.findById(carId).orElse(null);
        if (vehiculo == null || !vehiculo.getOwnerId().equals(clienteLogueado.getId())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No tienes permiso para eliminar este vehículo.");
            return "redirect:/portal?tab=perfil";
        }

        if ("VENDIDO".equalsIgnoreCase(vehiculo.getSalesStatus())) {
            redirectAttributes.addFlashAttribute("mensajeError", "No es posible eliminar un vehículo con venta finalizada.");
            return "redirect:/portal?tab=perfil";
        }

        try {
            vehicleRepository.delete(vehiculo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Vehículo eliminado de tu lista.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se puede eliminar este vehículo porque tiene procesos asociados.");
        }

        return "redirect:/portal?tab=perfil";
    }

    @PostMapping("/portal/profile/update")
    public String actualizarPerfilCliente(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User clienteSesion = (User) session.getAttribute("clienteLogueado");
        if (clienteSesion == null) {
            return "redirect:/login";
        }

        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("mensajeError", "El nombre y el correo electrónico son obligatorios.");
            return "redirect:/portal?tab=perfil";
        }

        User usuarioDb = userRepository.findById(clienteSesion.getId()).orElse(null);
        if (usuarioDb == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "No se encontró el usuario en el sistema.");
            return "redirect:/portal?tab=perfil";
        }

        usuarioDb.setName(name.trim());
        usuarioDb.setEmail(email.trim());
        usuarioDb.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : "");

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("mensajeError", "La nueva contraseña y su confirmación no coinciden.");
                return "redirect:/portal?tab=perfil";
            }
            usuarioDb.setPassword(newPassword);
        }

        try {
            User guardado = userRepository.save(usuarioDb);
            session.setAttribute("clienteLogueado", guardado);
            redirectAttributes.addFlashAttribute("mensajeExito", "Tus datos de perfil se actualizaron correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensajeError", "No fue posible guardar los cambios: " + e.getMessage());
        }

        return "redirect:/portal?tab=perfil";
    }

}