package cl.belmontespa.transporte.dto.request;

import jakarta.validation.constraints.NotNull;

public record ActivoDTORequest(@NotNull Boolean activo) {
}
