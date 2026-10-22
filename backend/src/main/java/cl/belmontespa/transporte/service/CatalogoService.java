package cl.belmontespa.transporte.service;

import cl.belmontespa.transporte.dto.response.CatalogosDTOResponse;
import cl.belmontespa.transporte.repository.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private final BancoRepository bancos;
    private final MedioPagoRepository medios;
    private final TipoReciboRepository recibos;
    private final ComunaRepository comunas;

    public CatalogoService(BancoRepository bancos, MedioPagoRepository medios, TipoReciboRepository recibos, ComunaRepository comunas) {
        this.bancos = bancos; this.medios = medios; this.recibos = recibos; this.comunas = comunas;
    }

    @Transactional(readOnly = true)
    public CatalogosDTOResponse obtener() {
        var porRegion = comunas.findAll(Sort.by("nombre")).stream().collect(Collectors.groupingBy(
                c -> c.getRegion().getNombre(), () -> new java.util.TreeMap<String, java.util.List<String>>(
                        java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es-CL"))),
                Collectors.mapping(c -> c.getNombre(), Collectors.toList())));
        return new CatalogosDTOResponse(
                bancos.findAll(Sort.by("nombre")).stream().map(b -> new CatalogosDTOResponse.Banco(b.getNombre(), b.getCodSbif())).toList(),
                medios.findAll(Sort.by("nombre")).stream().map(m -> m.getNombre()).toList(),
                recibos.findAll(Sort.by("nombre")).stream().map(r -> r.getNombre()).toList(),
                porRegion.entrySet().stream().map(e -> new CatalogosDTOResponse.Region(e.getKey(), e.getValue())).toList());
    }
}
