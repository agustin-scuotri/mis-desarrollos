package com.desarrollos.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Elimina el UNIQUE constraint heredado de la relación @OneToOne original
 * en documentos_convertidos.archivo_id, permitiendo que un archivo tenga
 * múltiples facturas convertidas (relación @ManyToOne).
 * Hibernate ddl-auto=update no elimina constraints automáticamente.
 */
@Component
public class DatabaseMigration implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropUniqueConstraintArchivoId();
    }

    private void dropUniqueConstraintArchivoId() {
        String sql = """
                DO $$
                DECLARE
                    v_constraint text;
                BEGIN
                    SELECT tc.constraint_name INTO v_constraint
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                    WHERE tc.table_name   = 'documentos_convertidos'
                      AND kcu.column_name = 'archivo_id'
                      AND tc.constraint_type = 'UNIQUE'
                    LIMIT 1;

                    IF v_constraint IS NOT NULL THEN
                        EXECUTE 'ALTER TABLE documentos_convertidos DROP CONSTRAINT ' || v_constraint;
                    END IF;
                END $$;
                """;
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // La constraint ya fue eliminada o no existía — sin acción necesaria
        }
    }
}
