package cl.belmontespa.transporte.config;

import cl.belmontespa.transporte.validation.Reglas;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ClavesDeRutaConfig implements WebMvcConfigurer {

	private static final Map<String, Predicate<String>> FORMATOS = Map.of(
			"folio", Reglas::esFolio,
			"hash", Reglas::esHashPago,
			"dniHash", Reglas::esDniHash);

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
			@Override
			@SuppressWarnings("unchecked")
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				Map<String, String> variables = (Map<String, String>) request
						.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
				if (variables != null) {
					FORMATOS.forEach((nombre, valido) -> {
						String valor = variables.get(nombre);
						if (valor != null && !valido.test(valor)) {
							throw new ResponseStatusException(HttpStatus.NOT_FOUND);
						}
					});
				}
				return true;
			}
		}).addPathPatterns("/api/v1/**");
	}
}
