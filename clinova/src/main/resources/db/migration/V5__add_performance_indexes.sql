-- Flyway migration V5: Performance Indexes for Clinova MySQL Database

CREATE INDEX idx_doc_kawak_id ON clinova_db.documentos (kawak_id);
CREATE INDEX idx_doc_codigo ON clinova_db.documentos (codigo);
CREATE INDEX idx_doc_estado ON clinova_db.documentos (estado);
CREATE INDEX idx_doc_proceso ON clinova_db.documentos (proceso);
CREATE INDEX idx_doc_tipo ON clinova_db.documentos (tipo);

CREATE INDEX idx_hv_cedula ON clinova_db.hojas_vida (cedula);
CREATE INDEX idx_hv_kawak_id ON clinova_db.hojas_vida (kawak_id);
CREATE INDEX idx_hv_estado ON clinova_db.hojas_vida (estado);
CREATE INDEX idx_hv_usuario_id ON clinova_db.hojas_vida (usuario_id);

CREATE INDEX idx_sop_hv_id ON clinova_db.soportes (hoja_vida_id);
CREATE INDEX idx_sop_tipo ON clinova_db.soportes (tipo_documento);

CREATE INDEX idx_usr_username ON clinova_db.usuarios (username);
CREATE INDEX idx_usr_persona ON clinova_db.usuarios (persona_id);
