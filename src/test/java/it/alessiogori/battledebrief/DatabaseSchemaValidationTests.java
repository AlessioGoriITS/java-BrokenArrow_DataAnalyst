package it.alessiogori.battledebrief;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:schema_validation;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
                + "INIT=RUNSCRIPT FROM 'database/schema.sql'",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "app.catalog.initialize=false"
})
@ActiveProfiles("test")
class DatabaseSchemaValidationTests {

    @Test
    void schemaMatchesTheJpaModel() {
    }
}
