package cl.belmontespa.transporte.controller;

import cl.belmontespa.transporte.dto.request.AsignarViajeDTORequest;
import cl.belmontespa.transporte.dto.request.CambiarEstadoViajeDTORequest;
import cl.belmontespa.transporte.dto.request.ViajeDTORequest;
import cl.belmontespa.transporte.dto.response.ViajeDTOResponse;
import cl.belmontespa.transporte.service.CrudService;
import cl.belmontespa.transporte.service.FiltroViajes;
import cl.belmontespa.transporte.service.ViajeService;
import cl.belmontespa.transporte.validation.Reglas;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// GET según rol: Administrador todos, Solicitante los propios y Conductor su agenda, con los
// filtros ?estado= y ?fecha=AAAA-MM-DD (FiltroViajes). Conductor, móvil y estado cambian solo por
// las acciones de negocio.
@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController extends CrudController<ViajeDTORequest, ViajeDTOResponse> {

	private final ViajeService viajeService;

	public ViajeController(ViajeService viajeService) {
		this.viajeService = viajeService;
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
		FiltroViajes filtro = FiltroViajes.de(filtros);
		if (tieneRol(auth, SOL)) {
			return viajeService.listarDeSolicitante(auth.getName(), filtro);
		}
		if (tieneRol(auth, CON)) {
			return viajeService.listarDelConductor(auth.getName(), filtro).stream()
					.map(viaje -> paraRol(viaje, auth)).toList();
		}
		return viajeService.listarDelAdministrador(filtro);
	}

	// Al Conductor no le llega el pago del viaje (README, "Recursos y roles").
	@Override
	protected ViajeDTOResponse paraRol(ViajeDTOResponse viaje, Authentication auth) {
		return tieneRol(auth, CON) ? viaje.sinPago() : viaje;
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
		return paraRol(viajeService.cambiarEstado(folio, request, auth.getName()), auth);
	}

	// CU-08/RF15: solo el Solicitante dueño del viaje.
	@PostMapping("/{folio}/cancelar")
	public ViajeDTOResponse cancelar(@PathVariable String folio, Authentication auth) {
		return viajeService.cancelar(folio, auth.getName());
	}
}
