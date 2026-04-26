package com.nexusfin.equity.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Shared base for Spring Boot integration tests. Holds only the annotations common across
 * every IT — no @MockBean — so subclasses control which mocks land in their Spring TestContext
 * cache key.
 *
 * Migration policy:
 *   - This class declares NO mocks. Beans go on specialized intermediate bases
 *     (e.g. {@link AbstractYunkaXiaohuaIT}) that group ITs with identical mock sets.
 *   - IT-specific service mocks stay on the individual IT classes.
 *   - Subclasses may add @ActiveProfiles to opt into a different profile group.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
}
