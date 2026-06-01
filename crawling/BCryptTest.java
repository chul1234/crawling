import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class BCryptTest {
  public static void main(String[] args) {
    System.out.println(new BCryptPasswordEncoder().encode("admin"));
  }
}
