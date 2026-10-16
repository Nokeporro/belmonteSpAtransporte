-- Datos iniciales: catálogos (README, "Catálogos en vez de CHECK"), división
-- político-administrativa de Chile y el Administrador inicial.
-- Los nombres de catálogo son los que usan los services (ConductorService, MovilService,
-- ViajeService, PagoService).

-- ---------------------------------------------------------------- catálogos

-- RF05/RF09 y CANCELADO (RF15/CU-08).
INSERT INTO estado_solicitud (nombre_estado)
VALUES ('PENDIENTE'), ('ASIGNADO'), ('EN_CURSO'), ('FINALIZADO'), ('CANCELADO');

INSERT INTO estado_conductor (estado) VALUES ('DISPONIBLE'), ('OCUPADO');

INSERT INTO estado_movil (nombre) VALUES ('DISPONIBLE'), ('EN_USO'), ('MANTENCION');

-- RF18/RF19.
INSERT INTO estado_pago (estado) VALUES ('PENDIENTE'), ('PAGADO'), ('RECHAZADO'), ('ANULADO');

INSERT INTO medio_pago (nombre)
VALUES ('TRANSFERENCIA'), ('TARJETA_DEBITO'), ('TARJETA_CREDITO'), ('EFECTIVO');

INSERT INTO tipo_recibo (nombre) VALUES ('BOLETA'), ('FACTURA');

-- Código SBIF (CMF) de cada banco.
INSERT INTO banco (nombre, cod_sbif) VALUES
  ('Banco de Chile', 1),
  ('Banco Internacional', 9),
  ('BancoEstado', 12),
  ('Scotiabank Chile', 14),
  ('Banco de Crédito e Inversiones', 16),
  ('Banco BICE', 28),
  ('HSBC Bank Chile', 31),
  ('Banco Santander-Chile', 37),
  ('Banco Itaú Chile', 39),
  ('Banco Security', 49),
  ('Banco Falabella', 51),
  ('Banco Ripley', 53),
  ('Banco Consorcio', 55),
  ('Banco BTG Pactual Chile', 59);

-- ---------------------------------------------------------------- regiones y comunas

-- 16 regiones y 346 comunas. UbicacionService busca la comuna por nombre + región; la
-- collation utf8mb4_unicode_ci ignora mayúsculas y tildes ("Nunoa" = "Ñuñoa").
INSERT INTO region (nombre) VALUES
  ('Arica y Parinacota'),
  ('Tarapacá'),
  ('Antofagasta'),
  ('Atacama'),
  ('Coquimbo'),
  ('Valparaíso'),
  ('Metropolitana de Santiago'),
  ('Libertador General Bernardo O''Higgins'),
  ('Maule'),
  ('Ñuble'),
  ('Biobío'),
  ('La Araucanía'),
  ('Los Ríos'),
  ('Los Lagos'),
  ('Aysén del General Carlos Ibáñez del Campo'),
  ('Magallanes y de la Antártica Chilena');

SET @r = (SELECT id_region FROM region WHERE nombre = 'Arica y Parinacota');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Arica', @r), ('Camarones', @r), ('Putre', @r), ('General Lagos', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Tarapacá');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Iquique', @r), ('Alto Hospicio', @r), ('Pozo Almonte', @r), ('Camiña', @r),
  ('Colchane', @r), ('Huara', @r), ('Pica', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Antofagasta');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Antofagasta', @r), ('Mejillones', @r), ('Sierra Gorda', @r), ('Taltal', @r),
  ('Calama', @r), ('Ollagüe', @r), ('San Pedro de Atacama', @r), ('Tocopilla', @r),
  ('María Elena', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Atacama');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Copiapó', @r), ('Caldera', @r), ('Tierra Amarilla', @r), ('Chañaral', @r),
  ('Diego de Almagro', @r), ('Vallenar', @r), ('Alto del Carmen', @r), ('Freirina', @r),
  ('Huasco', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Coquimbo');
INSERT INTO comuna (nombre, id_region) VALUES
  ('La Serena', @r), ('Coquimbo', @r), ('Andacollo', @r), ('La Higuera', @r),
  ('Paihuano', @r), ('Vicuña', @r), ('Illapel', @r), ('Canela', @r), ('Los Vilos', @r),
  ('Salamanca', @r), ('Ovalle', @r), ('Combarbalá', @r), ('Monte Patria', @r),
  ('Punitaqui', @r), ('Río Hurtado', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Valparaíso');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Valparaíso', @r), ('Casablanca', @r), ('Concón', @r), ('Juan Fernández', @r),
  ('Puchuncaví', @r), ('Quintero', @r), ('Viña del Mar', @r), ('Isla de Pascua', @r),
  ('Los Andes', @r), ('Calle Larga', @r), ('Rinconada', @r), ('San Esteban', @r),
  ('La Ligua', @r), ('Cabildo', @r), ('Papudo', @r), ('Petorca', @r), ('Zapallar', @r),
  ('Quillota', @r), ('La Calera', @r), ('Hijuelas', @r), ('La Cruz', @r), ('Nogales', @r),
  ('San Antonio', @r), ('Algarrobo', @r), ('Cartagena', @r), ('El Quisco', @r),
  ('El Tabo', @r), ('Santo Domingo', @r), ('San Felipe', @r), ('Catemu', @r),
  ('Llaillay', @r), ('Panquehue', @r), ('Putaendo', @r), ('Santa María', @r),
  ('Quilpué', @r), ('Limache', @r), ('Olmué', @r), ('Villa Alemana', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Metropolitana de Santiago');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Santiago', @r), ('Cerrillos', @r), ('Cerro Navia', @r), ('Conchalí', @r),
  ('El Bosque', @r), ('Estación Central', @r), ('Huechuraba', @r), ('Independencia', @r),
  ('La Cisterna', @r), ('La Florida', @r), ('La Granja', @r), ('La Pintana', @r),
  ('La Reina', @r), ('Las Condes', @r), ('Lo Barnechea', @r), ('Lo Espejo', @r),
  ('Lo Prado', @r), ('Macul', @r), ('Maipú', @r), ('Ñuñoa', @r),
  ('Pedro Aguirre Cerda', @r), ('Peñalolén', @r), ('Providencia', @r), ('Pudahuel', @r),
  ('Quilicura', @r), ('Quinta Normal', @r), ('Recoleta', @r), ('Renca', @r),
  ('San Joaquín', @r), ('San Miguel', @r), ('San Ramón', @r), ('Vitacura', @r),
  ('Puente Alto', @r), ('Pirque', @r), ('San José de Maipo', @r), ('Colina', @r),
  ('Lampa', @r), ('Tiltil', @r), ('San Bernardo', @r), ('Buin', @r),
  ('Calera de Tango', @r), ('Paine', @r), ('Melipilla', @r), ('Alhué', @r),
  ('Curacaví', @r), ('María Pinto', @r), ('San Pedro', @r), ('Talagante', @r),
  ('El Monte', @r), ('Isla de Maipo', @r), ('Padre Hurtado', @r), ('Peñaflor', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Libertador General Bernardo O''Higgins');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Rancagua', @r), ('Codegua', @r), ('Coinco', @r), ('Coltauco', @r), ('Doñihue', @r),
  ('Graneros', @r), ('Las Cabras', @r), ('Machalí', @r), ('Malloa', @r), ('Mostazal', @r),
  ('Olivar', @r), ('Peumo', @r), ('Pichidegua', @r), ('Quinta de Tilcoco', @r),
  ('Rengo', @r), ('Requínoa', @r), ('San Vicente', @r), ('Pichilemu', @r),
  ('La Estrella', @r), ('Litueche', @r), ('Marchigüe', @r), ('Navidad', @r),
  ('Paredones', @r), ('San Fernando', @r), ('Chépica', @r), ('Chimbarongo', @r),
  ('Lolol', @r), ('Nancagua', @r), ('Palmilla', @r), ('Peralillo', @r), ('Placilla', @r),
  ('Pumanque', @r), ('Santa Cruz', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Maule');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Talca', @r), ('Constitución', @r), ('Curepto', @r), ('Empedrado', @r), ('Maule', @r),
  ('Pelarco', @r), ('Pencahue', @r), ('Río Claro', @r), ('San Clemente', @r),
  ('San Rafael', @r), ('Cauquenes', @r), ('Chanco', @r), ('Pelluhue', @r), ('Curicó', @r),
  ('Hualañé', @r), ('Licantén', @r), ('Molina', @r), ('Rauco', @r), ('Romeral', @r),
  ('Sagrada Familia', @r), ('Teno', @r), ('Vichuquén', @r), ('Linares', @r),
  ('Colbún', @r), ('Longaví', @r), ('Parral', @r), ('Retiro', @r), ('San Javier', @r),
  ('Villa Alegre', @r), ('Yerbas Buenas', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Ñuble');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Chillán', @r), ('Bulnes', @r), ('Chillán Viejo', @r), ('El Carmen', @r), ('Pemuco', @r),
  ('Pinto', @r), ('Quillón', @r), ('San Ignacio', @r), ('Yungay', @r), ('Quirihue', @r),
  ('Cobquecura', @r), ('Coelemu', @r), ('Ninhue', @r), ('Portezuelo', @r), ('Ránquil', @r),
  ('Treguaco', @r), ('San Carlos', @r), ('Coihueco', @r), ('Ñiquén', @r),
  ('San Fabián', @r), ('San Nicolás', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Biobío');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Concepción', @r), ('Coronel', @r), ('Chiguayante', @r), ('Florida', @r),
  ('Hualqui', @r), ('Lota', @r), ('Penco', @r), ('San Pedro de la Paz', @r),
  ('Santa Juana', @r), ('Talcahuano', @r), ('Tomé', @r), ('Hualpén', @r), ('Lebu', @r),
  ('Arauco', @r), ('Cañete', @r), ('Contulmo', @r), ('Curanilahue', @r),
  ('Los Álamos', @r), ('Tirúa', @r), ('Los Ángeles', @r), ('Antuco', @r), ('Cabrero', @r),
  ('Laja', @r), ('Mulchén', @r), ('Nacimiento', @r), ('Negrete', @r), ('Quilaco', @r),
  ('Quilleco', @r), ('San Rosendo', @r), ('Santa Bárbara', @r), ('Tucapel', @r),
  ('Yumbel', @r), ('Alto Biobío', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'La Araucanía');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Temuco', @r), ('Carahue', @r), ('Cunco', @r), ('Curarrehue', @r), ('Freire', @r),
  ('Galvarino', @r), ('Gorbea', @r), ('Lautaro', @r), ('Loncoche', @r), ('Melipeuco', @r),
  ('Nueva Imperial', @r), ('Padre Las Casas', @r), ('Perquenco', @r), ('Pitrufquén', @r),
  ('Pucón', @r), ('Saavedra', @r), ('Teodoro Schmidt', @r), ('Toltén', @r),
  ('Vilcún', @r), ('Villarrica', @r), ('Cholchol', @r), ('Angol', @r), ('Collipulli', @r),
  ('Curacautín', @r), ('Ercilla', @r), ('Lonquimay', @r), ('Los Sauces', @r),
  ('Lumaco', @r), ('Purén', @r), ('Renaico', @r), ('Traiguén', @r), ('Victoria', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Los Ríos');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Valdivia', @r), ('Corral', @r), ('Lanco', @r), ('Los Lagos', @r), ('Máfil', @r),
  ('Mariquina', @r), ('Paillaco', @r), ('Panguipulli', @r), ('La Unión', @r),
  ('Futrono', @r), ('Lago Ranco', @r), ('Río Bueno', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Los Lagos');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Puerto Montt', @r), ('Calbuco', @r), ('Cochamó', @r), ('Fresia', @r),
  ('Frutillar', @r), ('Los Muermos', @r), ('Llanquihue', @r), ('Maullín', @r),
  ('Puerto Varas', @r), ('Castro', @r), ('Ancud', @r), ('Chonchi', @r),
  ('Curaco de Vélez', @r), ('Dalcahue', @r), ('Puqueldón', @r), ('Queilén', @r),
  ('Quellón', @r), ('Quemchi', @r), ('Quinchao', @r), ('Osorno', @r),
  ('Puerto Octay', @r), ('Purranque', @r), ('Puyehue', @r), ('Río Negro', @r),
  ('San Juan de la Costa', @r), ('San Pablo', @r), ('Chaitén', @r), ('Futaleufú', @r),
  ('Hualaihué', @r), ('Palena', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Aysén del General Carlos Ibáñez del Campo');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Coyhaique', @r), ('Lago Verde', @r), ('Aysén', @r), ('Cisnes', @r), ('Guaitecas', @r),
  ('Cochrane', @r), ('O''Higgins', @r), ('Tortel', @r), ('Chile Chico', @r),
  ('Río Ibáñez', @r);

SET @r = (SELECT id_region FROM region WHERE nombre = 'Magallanes y de la Antártica Chilena');
INSERT INTO comuna (nombre, id_region) VALUES
  ('Punta Arenas', @r), ('Laguna Blanca', @r), ('Río Verde', @r), ('San Gregorio', @r),
  ('Cabo de Hornos', @r), ('Antártica', @r), ('Porvenir', @r), ('Primavera', @r),
  ('Timaukel', @r), ('Natales', @r), ('Torres del Paine', @r);

-- ---------------------------------------------------------------- administrador inicial

-- No hay autorregistro ni endpoint público que cree Administradores (README).
-- SOLO DESARROLLO: contraseña temporal 'Belmonte2026!' (hash BCrypt), válida 30 días desde
-- que se aplica esta migración. Reemplazarla con PUT /api/v1/usuarios/admin@belmontespa.cl;
-- una contraseña nueva anula la expiración (RF21).
INSERT INTO usuario (nombre, correo, password_hash, password_temporal_expira)
VALUES ('Administrador', 'admin@belmontespa.cl',
        '$2a$10$sK9zWNu2Ci.UW1lWqOok4OMqG7M0rhFbG7O2C9mEWv5QiajYZZHWu',
        NOW() + INTERVAL 30 DAY);

-- El trigger bi_administrador_rol registra el rol en usuario_rol_activo.
INSERT INTO administrador (id_usuario)
SELECT id_usuario FROM usuario WHERE correo = 'admin@belmontespa.cl';
