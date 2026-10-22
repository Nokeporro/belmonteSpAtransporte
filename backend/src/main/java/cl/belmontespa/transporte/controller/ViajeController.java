package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.AsignarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.CambiarEstadoViajeDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.dto.response.ViajeDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.ViajeService;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// GET según rol: Administrador todos (?estado=), Solicitante los propios, Conductor los del
// día (?fecha=AAAA-MM-DD). Conductor, móvil y estado cambian solo por las acciones de negocio.
@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController extends CrudController<ViajeDTORequest, ViajeDTOResponse> {

	private final ViajeService viajeService;
	private final Clock clock;

	public ViajeController(ViajeService viajeService, Clock clock) {
		this.viajeService = viajeService;
		this.clock = clock;
	}

	@Override
	protected CrudService<?, String, ViajeDTORequest, ViajeDTOResponse> service() {
		return viajeService;
	}

	@Override
	protected void validarClave(String folio) {
		exigirFormato(Reglas.esFolio(folio));
	}

	@Override
	protected List<ViajeDTOResponse> listarPara(Authentication auth, Map<String, String> filtros) {
		if (tieneRol(auth, SOL)) {
			return viajeService.listarDeSolicitante(auth.getName());
		}
		if (tieneRol(auth, CON)) {
			return viajeService.listarDelDia(auth.getName(), fecha(filtros.get("fecha")));
		}
		String estado = filtros.get("estado");
		return estado == null ? viajeService.listar() : viajeService.listarPorEstado(estado);
	}

	@Override
	protected void exigirAcceso(String folio, ViajeDTOResponse viaje, Authentication auth) {
		if (tieneRol(auth, SOL)) {
			exigirPropio(viaje.correoSolicitante(), auth);
		} else if (tieneRol(auth, CON)) {
			exigirPropio(viaje.correoConductor(), auth);
		}
	}

	@Override
	protected ViajeDTOResponse crearPara(ViajeDTORequest request, Authentication auth) {
		return viajeService.crear(request, auth.getName());
	}

	// CU-04/RF05.
	@PostMapping("/{folio}/asignar")
	public ViajeDTOResponse asignar(@PathVariable String folio, @Valid @RequestBody AsignarViajeDTORequest request) {
		return viajeService.asignar(folio, request);
	}

	// CU-05/RF09: solo el conductor asignado.
	@PostMapping("/{folio}/estado")
	public ViajeDTOResponse cambiarEstado(@PathVariable String folio,
			@Valid @RequestBody CambiarEstadoViajeDTORequest request, Authentication auth) {
		return viajeService.cambiarEstado(folio, request, auth.getName());
	}

	// CU-08/RF15: solo el Solicitante dueño del viaje.
	@PostMapping("/{folio}/cancelar")
	public ViajeDTOResponse cancelar(@PathVariable String folio, Authentication auth) {
		return viajeService.cancelar(folio, auth.getName());
	}

	// Sin `fecha`, la agenda es la de hoy en hora de Chile.
	private LocalDate fecha(String valor) {
		if (valor == null) {
			return LocalDate.now(clock);
		}
		try {
			return LocalDate.parse(valor);
		} catch (DateTimeParseException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fecha debe tener formato AAAA-MM-DD");
		}
	}
}
