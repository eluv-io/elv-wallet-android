package app.eluvio.wallet.screens.property.rows

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.data.entities.v2.DisplayFormat
import app.eluvio.wallet.data.entities.v2.display.DisplaySettings
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.NavigationEvent
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.spacer
import app.eluvio.wallet.screens.property.DynamicPageLayoutState
import app.eluvio.wallet.screens.property.DynamicPageLayoutState.CarouselItem
import app.eluvio.wallet.screens.property.items.CarouselItemCard
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.button_24
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.focusCapturingGroup
import app.eluvio.wallet.util.compose.focusCapturingLazyList
import app.eluvio.wallet.util.compose.focusTrap
import app.eluvio.wallet.util.compose.fromHex
import app.eluvio.wallet.util.compose.thenIf
import app.eluvio.wallet.util.compose.thenIfNotNull
import coil.compose.AsyncImage

private val CARD_HEIGHT = 120.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CarouselSection(
    item: DynamicPageLayoutState.Section.Carousel,
    modifier: Modifier = Modifier
) {
    val display = item.displaySettings
    if (display == null || item.items.isEmpty()) {
        return
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .thenIfNotNull(display.inlineBackgroundColor) { background(Color.fromHex(it)) }
    ) {
        if (display.inlineBackgroundImageUrl != null) {
            AsyncImage(
                model = display.inlineBackgroundImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopStart,
                modifier = Modifier.matchParentSize()
            )
        }
        Row {
            val showLogo = display.logoUrl != null
            if (showLogo) {
                Logo(display)
            }
            Column(
                Modifier
                    .focusTrap(FocusDirection.Left, FocusDirection.Right)
                    .focusGroup() // Required to make focusRestorer() work down the line
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Whether or not to add padding to the start of the row.
                val startPadding = if (showLogo) 30.dp else Overscan.horizontalPadding

                var selectedFilter by remember { mutableStateOf<AttributeAndValue?>(null) }
                val filterRowFocusRequester = remember { FocusRequester() }

                val title = display.title?.ifEmpty { null }
                val subtitle = display.subtitle?.ifEmpty { null }
                val hasTitleRow = title != null || subtitle != null
                if (hasTitleRow) {
                    TitleRow(
                        title,
                        subtitle,
                        item.viewAllNavigationEvent,
                        startPadding
                    )
                }
                if (item.filterAttribute != null) {
                    FilterSelectorRow(
                        selectedValue = selectedFilter?.second,
                        attributeValues = item.filterAttribute.values.map { it.value },
                        onValueSelected = { tag ->
                            selectedFilter = tag?.let { item.filterAttribute.id to it }
                        },
                        modifier = Modifier
                            .focusRequester(filterRowFocusRequester)
                            .padding(top = 8.dp),
                        item.viewAllNavigationEvent?.takeIf { !hasTitleRow },
                        startPadding = startPadding
                    )
                } else if (!hasTitleRow && item.viewAllNavigationEvent != null) {
                    ViewAllButton(
                        item.viewAllNavigationEvent,
                        modifier = Modifier.padding(start = Overscan.horizontalPadding)
                    )
                }

                // If we have at least one element in after the last spacer, add another one. It's
                // brittle and duplicates the logic above, but compose makes it annoyingly hard to
                // do any other way
                if (hasTitleRow || item.filterAttribute != null || item.viewAllNavigationEvent != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val exitFocusModifier = Modifier
                    .focusProperties {
                        exit = {
                            if (it == FocusDirection.Up && item.filterAttribute != null) {
                                // Prevent focus from skipping over the filter row
                                filterRowFocusRequester
                            } else {
                                FocusRequester.Default
                            }
                        }
                    }
                    .focusGroup() // Required to make focusRestorer() work down the line
                val filteredItems = rememberFilteredItems(item.items, selectedFilter)
                if (selectedFilter != null && filteredItems.isEmpty()) {
                    // This shouldn't happen on a properly configured tenant, but just in case,
                    // we want to make sure the row height stays relatively consistent.
                    Text(
                        text = "Nothing here... yet?",
                        modifier = Modifier
                            .padding(horizontal = Overscan.horizontalPadding)
                            .height(CARD_HEIGHT)
                            .wrapContentHeight(Alignment.CenterVertically)
                    )
                } else {
                    SectionItems(
                        display.displayFormat,
                        filteredItems,
                        startPadding,
                        modifier = exitFocusModifier
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionItems(
    displayFormat: DisplayFormat,
    filteredItems: List<CarouselItem>,
    startPadding: Dp,
    modifier: Modifier = Modifier
) {
    when (displayFormat) {
        DisplayFormat.GRID -> ItemGrid(
            filteredItems,
            startPadding,
            modifier = modifier
        )

        // A "banner" style section still supports all the customization options of a carousel,
        // but only displays one item per row.
        DisplayFormat.BANNER -> ItemGrid(
            filteredItems,
            startPadding,
            maxItemsInEachRow = 1,
            modifier = modifier
        )

        // Default to carousel if the display format is unknown
        DisplayFormat.UNKNOWN,
        DisplayFormat.CAROUSEL -> ItemRow(
            filteredItems,
            startPadding,
            modifier = modifier
        )
    }
}

@Composable
private fun Logo(displaySettings: DisplaySettings) {
    displaySettings.logoUrl ?: return

    val focusManager = LocalFocusManager.current
    // There's a bug where the entire row is skipped over when the logo is present because the first
    // focusable child is offset to the right. To work around this, we add a dummy surface to make
    // sure the row is considered in focus resolution.
    Surface(
        onClick = {/* no click action, just here to capture focus */ },
        modifier = Modifier
            // Doesn't have to be accurate, but Surface should start below the top of card row
            .padding(top = CARD_HEIGHT)
            // Doubles as the padding for the Logo/text. (size of 0.dp is never focusable)
            .size(width = Overscan.horizontalPadding, height = 1.dp)
            // Invisible, but still focusable
            .alpha(0f)
            .onFocusChanged {
                if (it.isFocused) {
                    // Immediately throw focus to the card row
                    focusManager.moveFocus(FocusDirection.Right)
                }
            },
        content = {}
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .then(
                if (displaySettings.logoText != null) {
                    Modifier.padding(top = 40.dp)
                } else {
                    Modifier
                        .padding(top = 16.dp)
                        .height(CARD_HEIGHT)
                }
            )
            .width(95.dp)
    ) {
        AsyncImage(
            model = displaySettings.logoUrl,
            contentDescription = "Logo"
        )
        displaySettings.logoText?.let { text ->
            Text(
                text,
                style = MaterialTheme.typography.label_24,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.TitleRow(
    title: String?,
    subtitle: String?,
    viewAllNavigationEvent: NavigationEvent?,
    startPadding: Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .padding(start = startPadding, end = Overscan.horizontalPadding)
    ) {
        title?.let {
            Text(
                it,
                style = MaterialTheme.typography.body_32,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        viewAllNavigationEvent?.let {
            ViewAllButton(it)
        }
    }

    subtitle?.let {
        Text(
            it,
            style = MaterialTheme.typography.body_32,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .padding(start = startPadding, end = Overscan.horizontalPadding, top = 4.dp)
        )
    }
}

@Composable
private fun ViewAllButton(
    navigationEvent: NavigationEvent,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    Surface(
        onClick = { navigator(navigationEvent) },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White)),
            focusedBorder = Border.None
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(3.dp)),
        modifier = modifier
    ) {
        Text(
            "VIEW ALL",
            style = MaterialTheme.typography.button_24.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemGrid(
    items: List<CarouselItem>,
    startPadding: Dp,
    modifier: Modifier = Modifier,
    maxItemsInEachRow: Int = Int.MAX_VALUE
) {
    val firstChildFocusRequester = remember { FocusRequester() }
    var firstChildPositioned by remember { mutableStateOf(false) }
    // TODO: make lazy
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = maxItemsInEachRow,
        modifier = modifier
            .fillMaxWidth()
            .focusCapturingGroup {
                if (firstChildPositioned) {
                    firstChildFocusRequester
                } else {
                    FocusRequester.Default
                }
            }
            .padding(start = startPadding, end = Overscan.horizontalPadding)
    ) {
        items.forEachIndexed { index, item ->
            key(item) {
                CarouselItemCard(
                    carouselItem = item,
                    cardHeight = CARD_HEIGHT,
                    modifier = Modifier.thenIf(index == 0) {
                        onGloballyPositioned {
                            firstChildPositioned = true
                        }
                            .focusRequester(firstChildFocusRequester)
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemRow(items: List<CarouselItem>, startPadding: Dp, modifier: Modifier = Modifier) {
    // The 'key' function prevents from focusRestorer() from breaking when crashing when
    // filteredItems changes.
    // From what I could tell it's kind of like 'remember' but for Composable.
    key(items) {
        val scrollState = rememberLazyListState()
        val childFocusRequesters = remember(items.size) { List(items.size) { FocusRequester() } }
        LazyRow(
            state = scrollState,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = startPadding, end = Overscan.horizontalPadding),
            modifier = modifier.focusCapturingLazyList(scrollState, childFocusRequesters)
        ) {
            itemsIndexed(items) { index, item ->
                CarouselItemCard(
                    carouselItem = item,
                    cardHeight = CARD_HEIGHT,
                    modifier = Modifier.focusRequester(childFocusRequesters[index])
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FilterSelectorRow(
    selectedValue: String?,
    attributeValues: List<String>,
    onValueSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewAllNavigationEvent: NavigationEvent?,
    startPadding: Dp
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(start = startPadding, end = Overscan.horizontalPadding),
        modifier = modifier.focusRestorer { firstItemFocusRequester },
    ) {
        // TODO: there might not always be an "All" option (e.g. Season 1/2/3 etc)
        item {
            FilterTab(
                text = "All",
                value = null,
                onSelected = onValueSelected,
                selected = selectedValue == null,
                modifier = Modifier.focusRequester(firstItemFocusRequester)
            )
        }
        items(attributeValues) { attributeValue ->
            FilterTab(
                text = attributeValue,
                selected = selectedValue == attributeValue,
                onSelected = onValueSelected
            )
        }

        viewAllNavigationEvent?.let {
            item { ViewAllButton(it) }
        }
        spacer(width = 20.dp)
    }
}

@Composable
private fun FilterTab(
    text: String,
    selected: Boolean,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    value: String? = text,
) {
    Surface(
        onClick = { onSelected(value) },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = if (selected) Color.White else Color(0xFF7B7B7B),
            pressedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        modifier = modifier
            .onFocusChanged {
                if (it.hasFocus) {
                    onSelected(value)
                }
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body_32,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun rememberFilteredItems(
    items: List<CarouselItem>,
    selectedFilter: Pair<String, String>?
): List<CarouselItem> {
    return remember(items, selectedFilter) {
        if (selectedFilter == null) {
            items
        } else {
            items
                .filterIsInstance<CarouselItem.Media>()
                .filter {
                    it.entity.attributes
                        .firstOrNull { attribute -> attribute.id == selectedFilter.first }
                        ?.values
                        ?.map { tag -> tag.value }
                        ?.contains(selectedFilter.second) == true
                }
        }
    }
}

typealias AttributeAndValue = Pair<String, String>
