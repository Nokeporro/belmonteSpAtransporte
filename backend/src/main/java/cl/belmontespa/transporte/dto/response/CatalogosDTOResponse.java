package cl.belmontespa.transporte.dto.response;

import java.util.List;

public record CatalogosDTOResponse(List<Banco> bancos, List<String> mediosPago,
        List<String> tiposRecibo, List<Region> regiones) {
    public record Banco(String nombre, Integer codSbif) {}
    public record Region(String nombre, List<String> comunas) {}
}
