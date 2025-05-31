package devforge.servicio.impl;

import devforge.model.Usuario;
import devforge.repositorio.UsuarioRepositorio;
import devforge.servicio.UsuarioServicio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioServicioImpl implements UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServicioImpl(UsuarioRepositorio usuarioRepositorio, PasswordEncoder passwordEncoder) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepositorio.save(usuario);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepositorio.findAll();
    }

    @Override
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepositorio.findByUsername(username);
    }

    @Override
    public void eliminar(Long id) {
        usuarioRepositorio.deleteById(id);
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepositorio.findById(id).orElse(null);
    }

    @Override
    public Usuario buscarPorUsername(String username) {
        return usuarioRepositorio.findByUsername(username);
    }

    @Override
    public boolean validarPassword(Usuario usuario, String password) {
        return passwordEncoder.matches(password, usuario.getPassword());
    }

    // ... otros métodos existentes ...
} 