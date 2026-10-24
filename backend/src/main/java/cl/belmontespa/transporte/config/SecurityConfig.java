package cl.belmontespa.transporte.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

// API stateless con JWT (RNF01). Cada ruta declara sus roles (README, "Mapeo de roles por
// endpoint"); lo no listado se rechaza. "Solo lo propio" se valida en los controllers.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String ADM = "ADMINISTRADOR";
	private static final String CON = "CONDUCTOR";
	private static final String SOL = "SOLICITANTE";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, tools.jackson.databind.ObjectMapper json) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(rolDelToken())))
				.addFilterAfter(new PasswordTemporalFilter(json), org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error").permitAll()
						.requestMatchers(POST, "/api/v1/auth/login", "/api/v1/auth/recuperar-password").permitAll()
						// Autorregistro del Solicitante (RF01): aún no tiene JWT.
						.requestMatchers(POST, "/api/v1/solicitantes").permitAll()
						// Abordaje del Pasajero (RF28): no tiene cuenta, se identifica con el QR del vehículo y su documento.
						.requestMatchers(POST, "/api/v1/abordajes").permitAll()
						.requestMatchers(POST, "/api/v1/auth/logout").authenticated()
						.requestMatchers(GET, "/api/v1/catalogos").hasAnyRole(ADM, CON, SOL)

						// Sin POST: las cuentas nacen por su rol (RF01, CU-07 y el alta de administradores).
						.requestMatchers(GET, "/api/v1/usuarios", "/api/v1/usuarios/*").hasAnyRole(ADM, CON, SOL)
						.requestMatchers(PUT, "/api/v1/usuarios/*").hasAnyRole(ADM, SOL)

						.requestMatchers("/api/v1/administradores/**").hasRole(ADM)

						.requestMatchers(GET, "/api/v1/conductores/*").hasAnyRole(ADM, CON)
						.requestMatchers(GET, "/api/v1/conductores").hasRole(ADM)
						.requestMatchers(POST, "/api/v1/conductores").hasRole(ADM)
						.requestMatchers(PUT, "/api/v1/conductores/*").hasRole(ADM)

						.requestMatchers(GET, "/api/v1/solicitantes/*").hasAnyRole(ADM, SOL)
						.requestMatchers(GET, "/api/v1/solicitantes").hasRole(ADM)

						.requestMatchers(GET, "/api/v1/empresas/**").hasRole(ADM)

						.requestMatchers("/api/v1/moviles/**", "/api/v1/habilitaciones-movil-conductor/**").hasRole(ADM)

						.requestMatchers(POST, "/api/v1/pagos", "/api/v1/pagos/*/estado").hasRole(ADM)
						.requestMatchers(GET, "/api/v1/pagos/**").hasAnyRole(ADM, SOL)

						.requestMatchers(POST, "/api/v1/pasajeros", "/api/v1/pasajeros-viaje").hasRole(SOL)
						.requestMatchers(GET, "/api/v1/pasajeros/**").hasRole(ADM)
						.requestMatchers(GET, "/api/v1/pasajeros-viaje/**").hasAnyRole(ADM, SOL)

						.requestMatchers(POST, "/api/v1/viajes/*/asignar").hasRole(ADM)
						.requestMatchers(POST, "/api/v1/viajes/*/estado").hasRole(CON)
						.requestMatchers(POST, "/api/v1/viajes/*/rechazar").hasRole(CON)
						.requestMatchers(POST, "/api/v1/viajes").hasRole(SOL)
						.requestMatchers(POST, "/api/v1/viajes/*/cancelar").hasAnyRole(ADM, SOL)
						.requestMatchers(GET, "/api/v1/viajes/**").hasAnyRole(ADM, CON, SOL)

						.anyRequest().denyAll())
				.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProvider jwtProvider) {
		return jwtProvider.decoder();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// Claim "rol" → ROLE_<rol>; el nombre del Authentication es el sub (correo).
	private static JwtAuthenticationConverter rolDelToken() {
		JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
		roles.setAuthoritiesClaimName("rol");
		roles.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(roles);
		return converter;
	}
}
