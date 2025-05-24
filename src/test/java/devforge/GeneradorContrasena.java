
package devforge;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GeneradorContrasena {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("12345"));
    }
}