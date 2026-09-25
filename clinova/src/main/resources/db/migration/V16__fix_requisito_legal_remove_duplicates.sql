-- V16__fix_requisito_legal_remove_duplicates.sql
-- Eliminar los registros duplicados con ID 1-8 que no existen en Kawak
-- En Kawak la matriz de requisitos legales empieza en el ID 9
DELETE FROM requisito_legal WHERE id BETWEEN 1 AND 8;
