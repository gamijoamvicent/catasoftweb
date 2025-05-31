package devforge.servicio;

import devforge.model.Usuario;
import java.util.List;

public interface UsuarioServicio {
    Usuario guardar(Usuario usuario);
    List<Usuario> listarUsuarios();
    Usuario obtenerPorUsername(String username);
    void eliminar(Long id);
    Usuario obtenerPorId(Long id);
    Usuario buscarPorUsername(String username);
    boolean validarPassword(Usuario usuario, String password);
    List<Usuario> listarUsuariosPorLicoreria(Long licoreriaId);
    void eliminarUsuariosPorLicoreria(Long licoreriaId);
}