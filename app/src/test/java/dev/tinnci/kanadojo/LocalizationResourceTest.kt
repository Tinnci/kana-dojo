package dev.tinnci.kanadojo

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourceTest {
    @Test
    fun simplifiedChineseStringsCoverEveryBaseString() {
        val base = stringResources("values/strings.xml")
        val zh = stringResources("values-zh-rCN/strings.xml")

        assertEquals(base.keys, zh.keys)
    }

    @Test
    fun translatedStringsKeepFormatPlaceholders() {
        val base = stringResources("values/strings.xml")
        val zh = stringResources("values-zh-rCN/strings.xml")

        base.forEach { (key, value) ->
            assertEquals("placeholder mismatch for $key", placeholdersIn(value), placeholdersIn(zh.getValue(key)))
        }
    }

    @Test
    fun androidLocaleConfigAdvertisesEnglishAndSimplifiedChinese() {
        val locales = localeConfigTags()

        assertEquals(setOf("en", "zh-CN"), locales)
    }

    private fun stringResources(path: String): Map<String, String> {
        val document = parseXml(resourceFile(path))
        val strings = document.getElementsByTagName("string")
        return buildMap {
            repeat(strings.length) { index ->
                val element = strings.item(index) as Element
                val name = element.getAttribute("name")
                assertTrue("string resource without name in $path", name.isNotBlank())
                put(name, element.textContent)
            }
        }
    }

    private fun localeConfigTags(): Set<String> {
        val document = parseXml(resourceFile("xml/locales_config.xml"))
        val locales = document.getElementsByTagName("locale")
        return buildSet {
            repeat(locales.length) { index ->
                val element = locales.item(index) as Element
                add(element.getAttributeNS("http://schemas.android.com/apk/res/android", "name"))
            }
        }
    }

    private fun parseXml(file: File) =
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)

    private fun resourceFile(path: String): File {
        val userDir = File(requireNotNull(System.getProperty("user.dir")))
        return generateSequence(userDir) { it.parentFile }
            .flatMap { root ->
                sequenceOf(
                    File(root, "app/src/main/res/$path"),
                    File(root, "src/main/res/$path")
                )
            }
            .firstOrNull { it.isFile }
            ?: error("Cannot find Android resource $path from $userDir")
    }

    private fun placeholdersIn(value: String): Set<String> =
        Regex("%(?:\\d+\\$)?[sd]").findAll(value).map { it.value }.toSet()
}
