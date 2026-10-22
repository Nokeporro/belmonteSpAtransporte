package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.belmontespa.transporte.dto.request.PasajeroDTORequest;
import cl.belmontespa.transporte.model.Pasajero;
import cl.belmontespa.transporte.repository.PasajeroRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasajeroService: hash del DNI")
class PasajeroServiceTest {

	private static final String CLAVE = "clave-hmac-de-prueba";

	private PasajeroRepository repositorio;
	private PasajeroService servicio;

	@BeforeEach
	void preparar() {
		repositorio = mock(PasajeroRepository.class);
		when(repositorio.findByDniHash(any())).thenReturn(Optional.empty());
		when(repositorio.save(any(Pasajero.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		servicio = new PasajeroService(repositorio, CLAVE);
	}

	private String hash(PasajeroService servicio, String dni) {
		return servicio.hashDni(dni);
	}

	@Test
	@DisplayName("es el HMAC-SHA256 del documento normalizado, en Base64url sin relleno (43 caracteres)")
	void formato() throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(CLAVE.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String esperado = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(mac.doFinal("123456785".getBytes(StandardCharsets.UTF_8)));

		String obtenido = hash(servicio, "12.345.678-5");

		assertThat(obtenido).isEqualTo(esperado).hasSize(43).matches("[A-Za-z0-9_-]{43}");
	}

	@Test
	@DisplayName("no depende de puntos, guion, espacios ni mayúsculas")
	void normaliza() {
		String base = hash(servicio, "12.345.678-K");

		assertThat(hash(servicio, "12345678-K")).isEqualTo(base);
		assertThat(hash(servicio, "12345678k")).isEqualTo(base);
		assertThat(hash(servicio, " 12 345 678 - k ")).isEqualTo(base);
	}

	@Test
	@DisplayName("documentos distintos dan hashes distintos y el hash no contiene el documento")
	void distingue() {
		String uno = hash(servicio, "12.345.678-5");
		String otro = hash(servicio, "12.345.678-6");

		assertThat(uno).isNotEqualTo(otro).doesNotContain("12345678");
	}

	@Test
	@DisplayName("con otra clave HMAC el mismo documento da otro hash")
	void dependeDeLaClave() {
		String conUna = hash(servicio, "12.345.678-5");
		String conOtra = hash(new PasajeroService(repositorio, CLAVE + "-distinta"), "12.345.678-5");

		assertThat(conUna).isNotEqualTo(conOtra);
	}

	@Test
	@DisplayName("si el documento ya existe devuelve ese pasajero sin guardar otro")
	void esIdempotente() {
		Pasajero existente = new Pasajero();
		existente.setDniHash("hash-existente");
		existente.setNombre("Ya registrado");
		when(repositorio.findByDniHash(any())).thenReturn(Optional.of(existente));

		var respuesta = servicio.crear(new PasajeroDTORequest("12.345.678-5", "Otro nombre"));

		assertThat(respuesta.dniHash()).isEqualTo("hash-existente");
		assertThat(respuesta.nombre()).isNull();
		verify(repositorio, never()).save(any(Pasajero.class));
	}
}
