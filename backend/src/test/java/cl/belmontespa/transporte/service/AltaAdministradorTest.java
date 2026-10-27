package cl.belmontespa.transporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cl.belmontespa.transporte.dto.request.AdministradorDTORequest;
import cl.belmontespa.transporte.repository.AdministradorRepository;
import cl.belmontespa.transporte.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AltaAdministradorTest {
    @Test void configuraLaDuracionDeLaTemporalEnHorasConElRelojDeChile() {
        var repos = mock(UsuarioRepository.class);
        when(repos.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        var reloj = Clock.fixed(Instant.parse("2026-10-24T15:00:00.123Z"), ZoneId.of("America/Santiago"));
        var encoder = new BCryptPasswordEncoder();
        var usuarios = new UsuarioService(repos, encoder, reloj);
        var servicio = new AdministradorService(mock(AdministradorRepository.class), usuarios, 2, mock(EntityManager.class));
        var cuenta = servicio.nuevaEntidad(new AdministradorDTORequest("Administrador", "admin@test.cl", null, "Inicial12345")).getUsuario();
        assertThat(cuenta.getPasswordHash()).isEqualTo("*");
        assertThat(cuenta.getPasswordTemporalExpira()).isEqualTo(LocalDateTime.now(reloj).plusHours(2).withNano(0));
        assertThat(encoder.matches("Inicial12345", cuenta.getPasswordTemporalHash())).isTrue();
    }
    @Test void noPermiteArrancarConVigenciaCeroONegativa() {
        for (long horas : new long[] {0, -1})
            assertThatThrownBy(() -> new AdministradorService(mock(AdministradorRepository.class), mock(UsuarioService.class), horas,
                    mock(EntityManager.class))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PASSWORD_TEMPORAL_HORAS");
    }
}
