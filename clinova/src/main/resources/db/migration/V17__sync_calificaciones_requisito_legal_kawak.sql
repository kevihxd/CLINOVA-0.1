-- V17__sync_calificaciones_requisito_legal_kawak.sql
-- Sincronización exacta de las calificaciones de la matriz de requisitos legales
-- extraídas directamente de Kawak (tabla mrl_requisito_oss, versión activa 3 'Coorporativa').

-- 1. Requisito con 'No Cumple' (nivel_cumplimiento = 0 en Kawak):
-- ID 107: Norma 3280 (Ruta Integral de Atención)
UPDATE requisito_legal SET calificacion = 'No Cumple' WHERE id = 107;

-- 2. Requisitos con 'Cumple Parcialmente' (nivel_cumplimiento = 1 en Kawak):
-- IDs: 104, 127, 132, 133, 138, 139, 140, 141, 142, 143, 144, 158, 159, 160
UPDATE requisito_legal SET calificacion = 'Cumple Parcialmente' WHERE id IN (
    104, 127, 132, 133, 138, 139, 140, 141, 142, 143, 144, 158, 159, 160
);

-- 3. Requisitos 'No Aplica' / Sin calificar (nivel_cumplimiento = NULL en Kawak, normas derogadas):
-- IDs: 26, 27, 28, 120, 145
UPDATE requisito_legal SET calificacion = 'No Aplica' WHERE id IN (
    26, 27, 28, 120, 145
);
