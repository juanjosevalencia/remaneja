package com.example.demo.control;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> registrarUsuario(@RequestBody User user) {
        User nuevoUsuario = userService.crearUsuario(user);
        if (nuevoUsuario == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<User>> listarUsuarios() {
        List<User> listaUsuarios = userService.listarUsuarios();
        return ResponseEntity.ok(listaUsuarios);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> obtenerUsuarioPorId(@PathVariable Integer userId) {
        return userService.obtenerUsuarioPorId(userId)
                .map(usuarioEncontrado -> ResponseEntity.ok(usuarioEncontrado))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> actualizarUsuario(@PathVariable Integer userId, @RequestBody User userActualizado) {
        return userService.actualizarUsuario(userId, userActualizado)
                .map(usuarioGuardado -> ResponseEntity.ok(usuarioGuardado))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Integer userId) {
        if (userService.eliminarUsuario(userId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}