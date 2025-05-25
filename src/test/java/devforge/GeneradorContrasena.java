
package devforge;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GeneradorContrasena {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("admin123"));
    }
}

/*los admin pueden ver  a los otros admin la idea es que 
 * no puedan verlos o en su defecto se bloque la accion de eliminarlo 
 * solo el super amin puede hacerlo
 */