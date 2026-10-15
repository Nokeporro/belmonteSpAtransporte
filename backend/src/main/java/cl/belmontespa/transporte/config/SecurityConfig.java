package cl.belmontespa.transporte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Por ahora solo el hash de contraseñas (RNF01); el SecurityFilterChain con JWT
// y el mapeo rol→endpoint se agregan en la etapa de seguridad.
@Configuration
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
