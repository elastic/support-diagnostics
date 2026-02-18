package co.elastic.support.gradle

import com.github.jk1.license.LicenseReportExtension
import com.github.jk1.license.ModuleData
import com.github.jk1.license.ProjectData
import com.github.jk1.license.render.ReportRenderer
import java.io.File
import java.util.*

/**
 * Custom renderer for the jk1 license-report plugin that generates a NOTICE.txt
 * from a NOTICE.template file. Each dependency is listed as:
 *
 *   Name under License
 *
 * The template must contain the placeholder `#GENERATED_NOTICES#` which
 * will be replaced with the sorted dependency listing.
 */
class NoticeRenderer(
    private val templatePath: String = "NOTICE.template",
    private val outputFileName: String = "NOTICE.txt"
) : ReportRenderer {

    override fun render(data: ProjectData) {
        val project = data.project
        val config = project.extensions.getByType(LicenseReportExtension::class.java)

        val templateFile = project.file(templatePath)
        if (!templateFile.exists()) {
            throw IllegalStateException("NOTICE template not found: ${templateFile.absolutePath}")
        }

        val entries = TreeSet(String.CASE_INSENSITIVE_ORDER)

        data.allDependencies.forEach { module ->
            val name = resolveModuleName(module)
            val license = resolveModuleLicense(module)
            entries.add("  $name under $license")
        }

        val template = templateFile.readText()
        val notices = entries.joinToString("\n")
        val outputFile = File(config.absoluteOutputDir, outputFileName)
        outputFile.writeText(template.replace("#GENERATED_NOTICES#", notices) + "\n")

        // Also write to project root so NOTICE.txt stays in sync
        val rootNotice = project.file(outputFileName)
        rootNotice.writeText(outputFile.readText())
    }

    companion object {
        private fun resolveModuleName(module: ModuleData): String {
            // Try POM name first
            for (pom in module.poms) {
                val name = pom.name?.trim()
                if (!name.isNullOrEmpty()) return name
            }
            // Try manifest name
            for (manifest in module.manifests) {
                val name = manifest.name?.trim()
                if (!name.isNullOrEmpty()) return name
            }
            // Fall back to group:name
            return "${module.group}:${module.name}"
        }

        private fun resolveModuleLicense(module: ModuleData): String {
            val licenseNames = linkedSetOf<String>()

            // Check POM licenses
            for (pom in module.poms) {
                for (license in pom.licenses) {
                    val name = license.name?.trim()
                    if (!name.isNullOrEmpty()) {
                        licenseNames.add(name)
                    }
                }
            }

            // Check manifest license
            if (licenseNames.isEmpty()) {
                for (manifest in module.manifests) {
                    val name = manifest.license?.trim()
                    if (!name.isNullOrEmpty()) {
                        licenseNames.add(name)
                    }
                }
            }

            return if (licenseNames.isEmpty()) "Unknown" else licenseNames.joinToString(" or ")
        }
    }
}
