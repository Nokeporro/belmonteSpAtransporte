package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.request.PasajeroDTORequest;
import cl.belmontespa.transporte.dto.response.PasajeroDTOResponse;
import cl.belmontespa.transporte.model.Pasajero;
import cl.belmontespa.transporte.repository.PasajeroRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasajeroService extends CrudService<Pasajero, String, PasajeroDTORequest, PasajeroDTOResponse> {

	private static final String HMAC = "HmacSHA256";

	private final PasajeroRepository pasajeroRepository;
	private final SecretKeySpec dniKey;

	public PasajeroService(PasajeroRepository pasajeroRepository, @Value("${app.dni.secret}") String dniSecret) {
		this.pasajeroRepository = pasajeroRepository;
		this.dniKey = new SecretKeySpec(dniSecret.getBytes(StandardCharsets.UTF_8), HMAC);
	}

	@Override
	protected JpaRepository<Pasajero, Long> repositorio() {
		return pasajeroRepository;
	}

	@Override
	protected Sort orden() {
		return Sort.by("nombre", "dniHash");
	}

	@Override
	protected Optional<Pasajero> buscar(String dniHash) {
		return pasajeroRepository.findByDniHash(dniHash);
	}

	// El cliente no conoce el hash: si el documento ya existe, devuelve ese pasajero.
	@Override
	@Transactional
	public PasajeroDTOResponse crear(PasajeroDTORequest request) {
		cl.belmontespa.transporte.validation.Documento.normalizar(request.tipoDocumento(), request.dni());
		return pasajeroRepository.findByDniHash(hashDni(request.dni()))
				.map(p -> new PasajeroDTOResponse(p.getDniHash(), null, null))
				.orElseGet(() -> super.crear(request));
	}

	@Override
	protected Pasajero nuevaEntidad(PasajeroDTORequest request) {
		Pasajero pasajero = new Pasajero();
		pasajero.setDniHash(hashDni(request.dni()));
		pasajero.setNombre(request.nombre());
		return pasajero;
	}

	@Override
	protected PasajeroDTOResponse aResponse(Pasajero pasajero) {
		return PasajeroDTOResponse.from(pasajero);
	}

	// HMAC-SHA256 del RUT/pasaporte normalizado, en Base64url sin padding (43 caracteres).
	public String hashDni(String dni) {
		String normalizado = dni.replaceAll("[.\\-\\s]", "").toUpperCase(Locale.ROOT);
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(dniKey);
			byte[] hash = mac.doFinal(normalizado.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular el hash del documento", e);
		}
	}
}
