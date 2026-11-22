package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User crearUsuario(User user) {
        if (user.getId() == null || userRepository.existsById(user.getId())) {
            return null;
        }
        return userRepository.save(user);
    }

    public List<User> listarUsuarios() {
        return userRepository.findAll();
    }

    public Optional<User> obtenerUsuarioPorId(Integer userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> actualizarUsuario(Integer userId, User userActualizado) {
        return userRepository.findById(userId).map(usuarioExistente -> {
            usuarioExistente.setName(userActualizado.getName());
            usuarioExistente.setPhoneNumber(userActualizado.getPhoneNumber());
            usuarioExistente.setEmail(userActualizado.getEmail());
            usuarioExistente.setAge(userActualizado.getAge());
            usuarioExistente.setPassword(userActualizado.getPassword());
            return userRepository.save(usuarioExistente);
        });
    }

    public Optional<User> autenticarUsuario(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        return listarUsuarios().stream()
                .filter(usuario -> usuario.getEmail() != null
                        && usuario.getEmail().equalsIgnoreCase(email.trim())
                        && password.equals(usuario.getPassword()))
                .findFirst();
    }

    public boolean eliminarUsuario(Integer userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }
}