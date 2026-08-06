package app.eluvio.wallet.data.entities.v2.display

import app.eluvio.wallet.data.entities.v2.MediaPageEntity
import app.eluvio.wallet.data.entities.v2.MediaPageSectionEntity
import app.eluvio.wallet.data.entities.v2.MediaPropertyEntity

/**
 * Resolves the card theme that applies to items in [section].
 * The Property defines a default theme, which a Page can override, which a Section can override.
 * Section items always inherit whatever the Section landed on.
 */
fun MediaPropertyEntity.resolveCardTheme(
    page: MediaPageEntity?,
    section: MediaPageSectionEntity?,
): CardThemeEntity? {
    val themeId = section?.displaySettings?.cardThemeId?.ifEmpty { null }
        ?: page?.cardThemeId?.ifEmpty { null }
        ?: cardThemeId?.ifEmpty { null }
        ?: return null
    return cardThemes[themeId]
}
