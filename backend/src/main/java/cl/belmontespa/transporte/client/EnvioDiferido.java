package cl.belmontespa.transporte.client;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
final class EnvioDiferido {

	private EnvioDiferido() {
	}

	static void ejecutar(Executor executor, String descripcion, Runnable tarea) {
		Runnable seguro = () -> {
			try {
				tarea.run();
			} catch (RuntimeException e) {
				log.warn("No se pudo {}: {}", descripcion, e.toString());
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					despachar(executor, descripcion, seguro);
				}
			});
		} else {
			despachar(executor, descripcion, seguro);
		}
	}

	private static void despachar(Executor executor, String descripcion, Runnable tarea) {
		try {
			executor.execute(tarea);
		} catch (RuntimeException e) {
			log.warn("No se pudo programar {}: {}", descripcion, e.toString());
		}
	}
}
