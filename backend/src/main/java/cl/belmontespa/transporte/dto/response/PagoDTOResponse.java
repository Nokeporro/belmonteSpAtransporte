package cl.belmontespa.transporte.dto.response;

import cl.belmontespa.transporte.model.Pago;
import java.time.LocalDateTime;

public record PagoDTOResponse(
		String hash,
		Integer monto,
		String estadoPago,
		String banco,
		Integer codSbif,
		String rutEmpresa,
		String medioPago,
		String tipoRecibo,
		LocalDateTime fechaPago,
		Integer montoReembolsado
) {

	public static PagoDTOResponse from(Pago pago) {
		return new PagoDTOResponse(
				pago.getHash(),
				pago.getMonto(),
				pago.getEstadoPago().getEstado(),
				pago.getBanco().getNombre(),
				pago.getBanco().getCodSbif(),
				pago.getEmpresa().getRut(),
				pago.getMedioPago().getNombre(),
				pago.getTipoRecibo().getNombre(),
				pago.getFechaPago(),
				pago.getMontoReembolsado());
	}
}
