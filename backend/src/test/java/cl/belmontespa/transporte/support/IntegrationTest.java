package cl.belmontespa.transporte.support;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ReiniciarEsquemaConfig.class, NotificacionesTestConfig.class})
public abstract class IntegrationTest {

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

	@Autowired
	private JdbcTemplate jdbcDeLaApi;

	protected Api api;

	@BeforeEach
	void iniciarApi() throws Exception {
		api = new Api(mvc, jdbcDeLaApi);
		CORREO.purgeEmailFromAllMailboxes();
		fcm.limpiar();
	}
}
