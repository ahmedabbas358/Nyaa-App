import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Convention plugin for standardized unit and integration testing configuration.
 */
class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.withType<Test> {
                useJUnitPlatform {
                    includeEngines("junit-jupiter", "junit-vintage")
                }
                testLogging {
                    events("passed", "skipped", "failed")
                }
            }
        }
    }
}
