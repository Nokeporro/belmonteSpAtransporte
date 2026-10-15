package cl.belmontespa.transporte.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Envío de la contraseña temporal (CU-13/RF21). Implementación provisoria: solo
// registra el envío, sin la contraseña; falta conectar SMTP.
@Slf4j
@Component
public class EmailClient {

	public void enviarPasswordTemporal(String correo, String passwordTemporal) {
		log.info("Contraseña temporal generada para {} (envío por correo no configurado)", correo);
	}
}
