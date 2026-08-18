package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity
import app.eluvio.wallet.testing.RealmTestRule
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmDictionaryOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CardThemeSchemaTest {
    @get:Rule
    val realmRule = RealmTestRule()
    private val realm get() = realmRule.realm

    @Test
    fun `card theme states survive a realm round trip`() {
        val theme = CardThemeEntity().apply {
            id = "theme1"
            borderWidth = 3
            inactive = CardThemeStateEntity().apply {
                borderColor = "#5b29d6"
                backgroundColor = "#a52c97"
                backgroundColorOpacity = 100
                backgroundColor2 = "#5b29d6"
                backgroundColor2Opacity = 50
                backgroundGradientAngle = 360
                gradient = true
            }
            active = CardThemeStateEntity().apply {
                borderColor = "#a52c97"
                backgroundColor = "#000000"
                backgroundColorOpacity = 0
            }
        }
        val property = MediaPropertyEntity().apply {
            id = "property1"
            cardThemeId = "theme1"
            cardThemes = realmDictionaryOf("theme1" to theme)
        }

        realm.writeBlocking { copyToRealm(property) }

        val saved = realm.query<MediaPropertyEntity>().find().single().cardThemes["theme1"]!!
        assertEquals(theme, saved)
        assertEquals(true, saved.inactive!!.gradient)
        assertEquals(360, saved.inactive!!.backgroundGradientAngle)
        assertEquals(50, saved.inactive!!.backgroundColor2Opacity)
        assertEquals(0, saved.active!!.backgroundColorOpacity)
        assertEquals(saved.inactive, saved.state(focused = false))
        assertEquals(saved.active, saved.state(focused = true))
    }
}
