package com.partidoya.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("migration-test")
class DatabaseMigrationIntegrationTest {
    @Test
    void createsAndValidatesTheCompleteSchema() {
    }
}
