package cl.belmontespa.transporte.support;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de los tests de integración: la aplicación completa (seguridad, servicios, JPA y los
 * triggers de MariaDB) detrás de MockMvc, contra el esquema {@code belmonte_test}.
 *
 * <p>Los triggers de V2 llevan parte de las reglas de negocio, por eso hace falta una
 * MariaDB real (la del {@code database/docker-compose.yaml}, puerto 3307). Preparación única,
 * con el contenedor arriba:
 *
 * <pre>
 * mariadb -h127.0.0.1 -P3307 -uroot -proot_dev -e "CREATE DATABASE IF NOT EXISTS belmonte_test
 *   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL ON belmonte_test.* TO 'belmonte'@'%';"
 * </pre>
 *
 * <p>En cada ejecución el esquema se limpia y se migra de nuevo (ver
 * {@link ReiniciarEsquemaConfig}); otra base se indica con {@code TEST_DB_URL}, siempre con
 * nombre terminado en {@code _test}. Todas las clases comparten un único contexto de Spring.
 *
 * <p>Los avisos externos no salen de la máquina: el correo va a un servidor SMTP en memoria
 * ({@link #CORREO}) y el push lo recoge {@link FcmGrabador}. Ambos se vacían antes de cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ReiniciarEsquemaConfig.class, NotificacionesTestConfig.class})
public abstract class IntegrationTest {

	/** Servidor SMTP en memoria, con puerto libre y compartido por toda la ejecución. */
	protected static final GreenMail CORREO = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

	static {
		CORREO.start();
		Runtime.getRuntime().addShutdownHook(new Thread(CORREO::stop));
	}

	@DynamicPropertySource
	static void configurarCorreo(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", () -> "localhost");
		registry.add("spring.mail.port", () -> CORREO.getSmtp().getPort());
		registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
		registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
	}

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected FcmGrabador fcm;

	protected Api api;

	@BeforeEach
	void iniciarApi() throws Exception {
		api = new Api(mvc);
		CORREO.purgeEmailFromAllMailboxes();
		fcm.limpiar();
	}
}
