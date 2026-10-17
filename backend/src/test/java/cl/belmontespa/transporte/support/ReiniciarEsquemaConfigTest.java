package cl.belmontespa.transporte.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Guarda de ReiniciarEsquemaConfig: nunca limpiar una base que no sea de pruebas")
class ReiniciarEsquemaConfigTest {

	@Test
	@DisplayName("acepta esquemas cuyo nombre termina en _test, con o sin parámetros")
	void aceptaEsquemasDePruebas() {
		assertThatCode(() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas("jdbc:mariadb://localhost:3307/belmonte_test"))
				.doesNotThrowAnyException();
		assertThatCode(() -> ReiniciarEsquemaConfig
				.exigirEsquemaDePruebas("jdbc:mariadb://localhost:3307/belmonte_test?user=belmonte&password=x"))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("rechaza la base de desarrollo y cualquier nombre que no termine en _test")
	void rechazaLoDemas() {
		assertThatThrownBy(() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas("jdbc:mariadb://localhost:3307/belmonte"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(
				() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas("jdbc:mariadb://localhost:3307/belmonte_test2"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas("jdbc:mariadb://localhost/belmonte_prod"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("un _test en los parámetros no engaña a la guarda")
	void ignoraParametros() {
		assertThatThrownBy(
				() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas("jdbc:mariadb://localhost:3307/belmonte?db=x_test"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("rechaza una URL nula")
	void rechazaNula() {
		assertThatThrownBy(() -> ReiniciarEsquemaConfig.exigirEsquemaDePruebas(null))
				.isInstanceOf(IllegalStateException.class);
	}
}
