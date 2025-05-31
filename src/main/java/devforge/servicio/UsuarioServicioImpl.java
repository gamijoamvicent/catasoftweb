package devforge.servicio;

import devforge.model.Usuario;
import devforge.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServicioImpl implements UsuarioServicio {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Usuario guardar(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public Usuario buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
            .orElse(null);
    }

    @Override
    public boolean validarPassword(Usuario usuario, String password) {
        return passwordEncoder.matches(password, usuario.getPassword());
    }

    @Override
    public List<Usuario> listarUsuariosPorLicoreria(Long licoreriaId) {
        return usuarioRepository.findByLicoreriaId(licoreriaId);
    }

    @Override
    @Transactional
    public void eliminarUsuariosPorLicoreria(Long licoreriaId) {
        List<Usuario> usuarios = usuarioRepository.findByLicoreriaId(licoreriaId);
        for (Usuario usuario : usuarios) {
            if (!usuario.getRol().equals(Usuario.Rol.SUPER_ADMIN)) {
                usuarioRepository.delete(usuario);
            }
        }
    }
} 