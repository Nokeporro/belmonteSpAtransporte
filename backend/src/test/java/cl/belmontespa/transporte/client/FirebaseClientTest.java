package cl.belmontespa.transporte.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import cl.belmontespa.transporte.support.FcmGrabador;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("FirebaseClient")
class FirebaseClientTest {

	private static final Map<String, String> DATOS = Map.of("folio", "VJ123");

	private FcmGrabador grabador;

	@BeforeEach
	void preparar() {
		grabador = new FcmGrabador();
	}

	@AfterEach
	void limpiarTransaccion() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private FirebaseClient cliente(FcmSender sender) {
		return new FirebaseClient(sender, Runnable::run);
	}

	@Test
	@DisplayName("envía título, mensaje y datos al token del dispositivo")
	void envia() {
		cliente(grabador).notificar("token-1", "Viaje asignado", "Tu viaje VJ123 ya tiene conductor", DATOS);

		assertThat(grabador.envios()).singleElement().satisfies(envio -> {
			assertThat(envio.token()).isEqualTo("token-1");
			assertThat(envio.titulo()).isEqualTo("Viaje asignado");
			assertThat(envio.mensaje()).isEqualTo("Tu viaje VJ123 ya tiene conductor");
			assertThat(envio.datos()).isEqualTo(DATOS);
		});
	}

	@Test
	@DisplayName("sin token registrado no envía nada")
	void sinToken() {
		FirebaseClient cliente = cliente(grabador);
		cliente.notificar(null, "t", "m", DATOS);
		cliente.notificar("", "t", "m", DATOS);
		cliente.notificar("   ", "t", "m", DATOS);

		assertThat(grabador.envios()).isEmpty();
	}

	@Test
	@DisplayName("sin credenciales de Firebase no falla")
	void sinCredenciales() {
		assertThatCode(() -> cliente(null).notificar("token-1", "t", "m", DATOS)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("un fallo del envío no llega al llamador")
	void falloNoSePropaga() {
		FcmSender roto = (token, titulo, mensaje, datos) -> {
			throw new IllegalStateException("sin red");
		};
		FcmSender sinRegistro = (token, titulo, mensaje, datos) -> {
			throw new FcmSender.FcmException(true, "UNREGISTERED", null);
		};

		assertThatCode(() -> cliente(roto).notificar("token-1", "t", "m", DATOS)).doesNotThrowAnyException();
		assertThatCode(() -> cliente(sinRegistro).notificar("token-1", "t", "m", DATOS)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("dentro de una transacción espera al commit para enviar")
	void esperaAlCommit() {
		TransactionSynchronizationManager.initSynchronization();

		cliente(grabador).notificar("token-1", "t", "m", DATOS);

		assertThat(grabador.envios()).isEmpty();
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
		assertThat(grabador.envios()).hasSize(1);
	}

	@Test
	@DisplayName("si la transacción se revierte no envía nada")
	void noEnviaSiSeRevierte() {
		TransactionSynchronizationManager.initSynchronization();

		cliente(grabador).notificar("token-1", "t", "m", DATOS);
		TransactionSynchronizationManager.getSynchronizations()
				.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

		assertThat(grabador.envios()).isEmpty();
	}
}
