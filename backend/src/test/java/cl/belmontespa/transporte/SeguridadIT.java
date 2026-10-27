package cl.belmontespa.transporte;

import static org.assertj.core.api.Assertions.assertThat;

import cl.belmontespa.transporte.support.Api;
import cl.belmontespa.transporte.support.Api.Cuenta;
import cl.belmontespa.transporte.support.IntegrationTest;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("Seguridad: JWT, roles y login")
class SeguridadIT extends IntegrationTest {

	@Value("${app.jwt.secret}")
	private String secretoJwt;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("sin token, un recurso protegido responde 401")
	void sinTokenResponde401() {
		assertThat(api.get("/viajes", null).status()).isEqualTo(401);
		assertThat(api.get("/usuarios", null).status()).isEqualTo(401);
		assertThat(api.post("/viajes", null, "{}").status()).isEqualTo(401);
	}

	@Test
	@DisplayName("un token alterado responde 401")
	void tokenAlteradoResponde401() {
		assertThat(api.get("/viajes", api.admin() + "x").status()).isEqualTo(401);
	}

	@Test
	@DisplayName("un token vencido responde 401")
	void tokenVencidoResponde401() {
		Instant ahora = Instant.now();
		String vencido = firmar(secretoJwt, Api.ADMIN_CORREO, "ADMINISTRADOR", ahora.minusSeconds(900),
				ahora.minusSeconds(600));
		assertThat(api.get("/viajes", vencido).status()).isEqualTo(401);
	}

	@Test
	@DisplayName("un token firmado con otra clave responde 401")
	void tokenConOtraClaveResponde401() {
		String otraClave = Base64.getEncoder()
				.encodeToString("otra-clave-distinta-de-32-bytes-o-mas".getBytes(StandardCharsets.UTF_8));
		Instant ahora = Instant.now();
		String falso = firmar(otraClave, Api.ADMIN_CORREO, "ADMINISTRADOR", ahora, ahora.plusSeconds(600));
		assertThat(api.get("/viajes", falso).status()).isEqualTo(401);
	}

	@Test
	@DisplayName("un token válido con un rol que la ruta no permite responde 403")
	void rolInsuficienteResponde403() {
		Cuenta conductor = api.conductor();
		Cuenta solicitante = api.solicitante();
		assertThat(api.get("/moviles", conductor.token()).status()).isEqualTo(403);
		assertThat(api.get("/moviles", solicitante.token()).status()).isEqualTo(403);
		assertThat(api.post("/viajes/NOEXISTE/asignar", solicitante.token(), "{}").status()).isEqualTo(403);
		// V8 es solo del conductor: ni el Administrador ni el Solicitante, y antes de validar el cuerpo.
		assertThat(api.post("/viajes/NOEXISTE/rechazar", solicitante.token(), "{}").status()).isEqualTo(403);
		assertThat(api.post("/viajes/NOEXISTE/rechazar", api.admin(), "{}").status()).isEqualTo(403);
		assertThat(api.post("/usuarios", solicitante.token(), "{}").status()).isEqualTo(403);
		assertThat(api.post("/viajes", conductor.token(), "{}").status()).isEqualTo(403);
	}

	@Test
	@DisplayName("una ruta que no está en la matriz de roles queda denegada incluso para el administrador")
	void rutaNoListadaResponde403() {
		assertThat(api.get("/regiones", api.admin()).status()).isEqualTo(403);
		assertThat(api.get("/ruta-que-no-existe", api.admin()).status()).isEqualTo(403);
	}

	@Test
	@DisplayName("la autorización se evalúa antes que la validación del cuerpo")
	void autorizacionAntesQueValidacion() {
		Cuenta solicitante = api.solicitante();
		assertThat(api.post("/moviles", solicitante.token(), "{}").status()).isEqualTo(403);
		assertThat(api.post("/moviles", api.admin(), "{}").status()).isEqualTo(400);
	}

	@Test
	@DisplayName("el login entrega un token Bearer con vigencia")
	void loginCorrecto() {
		Api.Resp resp = Api.exigir(api.iniciarSesion(Api.ADMIN_CORREO, Api.ADMIN_CLAVE), "login admin", 200);
		assertThat(resp.texto("$.accessToken")).isNotBlank();
		assertThat(resp.texto("$.tokenType")).isEqualToIgnoringCase("Bearer");
		assertThat(resp.entero("$.expiresIn")).isEqualTo(28_800);
		assertThat(resp.texto("$.correo")).isEqualTo(Api.ADMIN_CORREO);
	}

	@Test
	@DisplayName("clave incorrecta y correo inexistente responden 401 con el mismo mensaje")
	void loginFallidoNoRevelaCuentas() {
		Api.Resp claveMala = api.iniciarSesion(Api.ADMIN_CORREO, "claveMala99");
		Api.Resp sinCuenta = api.iniciarSesion("nadie@test.cl", "claveMala99");
		assertThat(claveMala.status()).isEqualTo(401);
		assertThat(sinCuenta.status()).isEqualTo(401);
		assertThat(claveMala.texto("$.message")).isEqualTo(sinCuenta.texto("$.message"));
	}

	@Test
	@DisplayName("un usuario sin rol no puede iniciar sesión")
	void usuarioSinRolNoInicia() {
		// Ninguna ruta crea usuarios sin rol (no hay POST /usuarios): se inserta por SQL.
		String correo = "sinrol-" + System.nanoTime() + "@test.cl";
		jdbc.update("INSERT INTO usuario (nombre, correo, password_hash) VALUES (?, ?, ?)",
				"Sin rol", correo, passwordEncoder.encode(Api.CLAVE));
		assertThat(api.iniciarSesion(correo, Api.CLAVE).status()).isEqualTo(401);
	}

	@Test
	@DisplayName("las rutas que el README ya no tiene responden 403: POST /usuarios, POST /empresas y PUT /solicitantes")
	void rutasQueSobranResponden403() {
		Cuenta solicitante = api.solicitante();
		String usuario = """
				{"nombre":"Otro","correo":"otro-%d@test.cl","password":"%s"}""".formatted(System.nanoTime(), Api.CLAVE);
		String empresa = """
				{"razonSocial":"Empresa","rut":"76543210-3"}""";
		assertThat(api.post("/usuarios", api.admin(), usuario).status()).isEqualTo(403);
		assertThat(api.post("/usuarios", solicitante.token(), usuario).status()).isEqualTo(403);
		assertThat(api.post("/empresas", solicitante.token(), empresa).status()).isEqualTo(403);
		assertThat(api.post("/empresas", api.admin(), empresa).status()).isEqualTo(403);
		assertThat(api.put("/solicitantes/" + solicitante.correo(), solicitante.token(),
				api.cuerpoSolicitante(solicitante.correo(), "76543210-3", "Empresa")).status()).isEqualTo(403);
	}

	@Test
	@DisplayName("el conductor no edita su cuenta por PUT /usuarios; el administrador y el solicitante sí")
	void conductorNoUsaPutUsuarios() {
		Cuenta conductor = api.conductor();
		String cuerpo = """
				{"nombre":"Conductor Renombrado","correo":"%s"}""".formatted(conductor.correo());
		assertThat(api.put("/usuarios/" + conductor.correo(), conductor.token(), cuerpo).status()).isEqualTo(403);

		Cuenta solicitante = api.solicitante();
		String propio = """
				{"nombre":"Solicitante Renombrado","correo":"%s"}""".formatted(solicitante.correo());
		assertThat(api.put("/usuarios/" + solicitante.correo(), solicitante.token(), propio).status())
				.isEqualTo(200);
	}

	@Test
	@DisplayName("el login valida el cuerpo y responde un ApiError con una línea por campo que falta")
	void loginInvalidoResponde400() {
		Api.Resp resp = api.post("/auth/login", null, "{}");
		assertThat(resp.status()).isEqualTo(400);
		assertThat(resp.entero("$.status")).isEqualTo(400);
		assertThat(resp.texto("$.path")).isEqualTo("/api/v1/auth/login");
		assertThat(resp.texto("$.message")).isEqualTo("correo: Es obligatorio.\npassword: Es obligatorio.");
	}

	@Test
	@DisplayName("un JSON mal formado responde 400")
	void jsonRotoResponde400() {
		assertThat(api.post("/auth/login", null, "{").status()).isEqualTo(400);
	}

	@Test
	@DisplayName("los endpoints públicos no piden token")
	void endpointsPublicos() {
		// POST /solicitantes es el autorregistro (RF01): sin token llega a la validación.
		assertThat(api.post("/solicitantes", null, "{\"correo\":\"x\"}").status()).isEqualTo(400);
		// Recuperar la contraseña es del Administrador (CU-13): sin token no llega a la validación.
		assertThat(api.post("/auth/recuperar-password", null, "{\"correo\":\"nadie@test.cl\"}").status())
				.isEqualTo(401);
	}

	@Test
	@DisplayName("el token solo sirve para el correo de su subject: no se puede editar una cuenta ajena")
	void noSePuedeEditarCuentaAjena() {
		Cuenta ana = api.solicitante();
		Cuenta beto = api.solicitante();
		String cuerpo = """
				{"nombre":"Hack","correo":"%s"}""".formatted(beto.correo());
		assertThat(api.put("/usuarios/" + beto.correo(), ana.token(), cuerpo).status()).isEqualTo(403);
		assertThat(api.put("/usuarios/" + beto.correo(), beto.token(), cuerpo).status()).isEqualTo(200);
	}

	private static String firmar(String base64Secret, String correo, String rol, Instant emitido, Instant expira) {
		var clave = new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(correo).claim("rol", rol).issuedAt(emitido)
				.expiresAt(expira).build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}
}
