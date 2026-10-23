package cl.belmontespa.transporte.dto.response;

import java.time.LocalDateTime;

// La marca guardada: repetir el abordaje devuelve la misma hora.
public record AbordajeDTOResponse(LocalDateTime fhAbordaje) {}
