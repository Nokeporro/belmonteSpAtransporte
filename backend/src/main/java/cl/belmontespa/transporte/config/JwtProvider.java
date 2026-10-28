package cl.belmontespa.transporte.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

	private final SecretKey key;
	private final JwtEncoder encoder;
	private final long expirationSeconds;

	public JwtProvider(
			@Value("${app.jwt.secret}") String base64Secret,
			@Value("${app.jwt.expiration-seconds:28800}") long expirationSeconds) {
		if (expirationSeconds < 1) {
			throw new IllegalArgumentException("La expiración del JWT debe ser positiva");
		}
		this.key = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
		this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		this.expirationSeconds = expirationSeconds;
	}

	public String generar(String correo, String rol) {
		return generar(correo, rol, false);
	}

	public String generar(String correo, String rol, boolean temporal) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(correo)
				.claim("rol", rol)
				.claim("temporal", temporal)
				.issuedAt(ahora)
				.expiresAt(ahora.plusSeconds(expirationSeconds))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public JwtDecoder decoder() {
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}
}
