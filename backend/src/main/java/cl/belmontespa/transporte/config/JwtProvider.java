package cl.belmontespa.transporte.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Emite y valida el JWT de CU-14: sub = correo, rol y exp.
@Component
public class JwtProvider {

	private final SecretKey signingKey;
	private final long expirationSeconds;

	public JwtProvider(
			@Value("${app.jwt.secret}") String base64Secret,
			@Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
		if (expirationSeconds < 1) {
			throw new IllegalArgumentException("La expiración del JWT debe ser positiva");
		}
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
		this.expirationSeconds = expirationSeconds;
	}

	public String generar(String correo, String rol) {
		Instant ahora = Instant.now();
		return Jwts.builder()
				.subject(correo)
				.claim("rol", rol)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(ahora.plusSeconds(expirationSeconds)))
				.signWith(signingKey)
				.compact();
	}

	// Lanza io.jsonwebtoken.JwtException si la firma no es válida o el token expiró.
	public Claims leerClaims(String token) {
		return Jwts.parser().verifyWith(signingKey).build()
				.parseSignedClaims(token).getPayload();
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}
}
