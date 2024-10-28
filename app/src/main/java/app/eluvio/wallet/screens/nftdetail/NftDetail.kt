package app.eluvio.wallet.screens.nftdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.network.dto.ContractInfoDto
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider
import app.eluvio.wallet.screens.dashboard.myitems.MediaCard
import app.eluvio.wallet.screens.destinations.FullscreenQRDialogDestination
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.carousel_48
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.util.compose.RealisticDevices
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@MainGraph
@Destination(navArgsDelegate = NftDetailNavArgs::class)
@Composable
fun NftDetail() {
    hiltViewModel<NftDetailViewModel>().subscribeToState { vm, state ->
        if (state.loading) {
            DelayedFullscreenLoader()
        } else {
            NftDetail(state)
        }
    }
}

@Composable
private fun NftDetail(state: NftDetailViewModel.State) {
    val media = state.media ?: return
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(50.dp),
            modifier = Modifier.fillMaxSize(0.7f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MediaCard(media = media, onClick = null, modifier = Modifier.weight(1f))
                if (media.propertyId != null) {
                    val navigator = LocalNavigator.current
                    TvButton(
                        text = "Go to Property",
                        onClick = { navigator(PropertyDetailDestination(propertyId = media.propertyId).asPush()) },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            NftMetadata(state)
        }
    }
}

@Composable
private fun ColumnScope.LabeledInfo(label: String, text: String, maxLines: Int = Int.MAX_VALUE) {
    Text(
        text = label,
        style = MaterialTheme.typography.body_32,
        color = Color(0xFF9B9B9B),
        modifier = Modifier.padding(bottom = 5.dp)
    )
    Text(
        text = text,
        style = MaterialTheme.typography.label_24.copy(
            fontWeight = FontWeight.Normal,
        ),
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        modifier = Modifier.padding(bottom = 15.dp)
    )
}

private enum class NftTabs(val title: String) {
    DESCRIPTION("Description") {
        @Composable
        override fun Content(state: NftDetailViewModel.State, scope: ColumnScope) {
            val media = state.media ?: return
            Column {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.carousel_48
                )
                Text(
                    text = "${media.subtitle}    #${media.tokenId}",
                    style = MaterialTheme.typography.label_24,
                    color = Color(0xFF7a7a7a),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(text = media.description, style = MaterialTheme.typography.label_24)
            }
        }
    },
    MINT_INFO("Mint Info") {
        @Composable
        override fun Content(state: NftDetailViewModel.State, scope: ColumnScope) {
            Column {
                if (state.contractInfo != null) {
                    LabeledInfo("Edition", state.media?.subtitle ?: "")
                    LabeledInfo("Number Minted", state.contractInfo.minted.toString())
                    LabeledInfo("Number in Circulation", state.contractInfo.totalSupply.toString())
                    LabeledInfo("Number Burned", state.contractInfo.burned.toString())
                    LabeledInfo(
                        "Maximum Possible in Circulation",
                        (state.contractInfo.cap - state.contractInfo.burned).toString()
                    )
                    LabeledInfo("Cap", state.contractInfo.cap.toString())
                }
            }
        }
    },
    CONTRACT_VERSION("Contract & Version") {
        @Composable
        override fun Content(state: NftDetailViewModel.State, scope: ColumnScope) = scope.run {
            LabeledInfo("Contract Address", state.media?.contractAddress ?: "", maxLines = 1)
            LabeledInfo("Hash", state.media?.versionHash ?: "", maxLines = 1)

            if (state.lookoutUrl != null) {
                val navigator = LocalNavigator.current
                TvButton(
                    "See more info on Eluvio Lookout",
                    onClick = {
                        navigator(
                            FullscreenQRDialogDestination(
                                url = state.lookoutUrl,
                                title = "See More Info on Eluvio Lookout"
                            ).asPush()
                        )
                    },
                    Modifier.padding(vertical = 20.dp)
                )
            }
        }
    };

    @Composable
    abstract fun Content(state: NftDetailViewModel.State, scope: ColumnScope)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NftMetadata(state: NftDetailViewModel.State) {
    val tabs = NftTabs.entries
    var selectedTab by rememberSaveable { mutableStateOf(tabs.first()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .focusRestorer()
                .focusGroup()
        ) {
            tabs.forEach { tab ->
                MetadataTab(tab.title, selectedTab == tab, { selectedTab = tab })
            }
        }
        selectedTab.Content(state, scope = this)
    }
}

@Composable
private fun MetadataTab(
    text: String,
    selected: Boolean,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    value: String? = text,
) {
    Surface(
        onClick = { onSelected(value) },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = Color(0xFF7B7B7B),
            focusedContentColor = Color.White,
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
            maxLines = 1,
            modifier = Modifier
                .animatedUnderline(selected)
                // Give underline some space below text
                .padding(bottom = 2.dp)
        )
    }
}

/**
 * Only works for 1 line of text.
 */
private fun Modifier.animatedUnderline(visible: Boolean) = composed {
    val lineWidthFraction by animateFloatAsState(
        targetValue = if (visible) 1f else 0f, label = "underlineSize"
    )
    val color = LocalContentColor.current
    drawWithCache {
        onDrawBehind {
            val strokeWidthPx = 2.dp.toPx()
            val verticalOffset = size.height
            val lineWidth = size.width * lineWidthFraction
            drawLine(
                color = color,
                strokeWidth = strokeWidthPx,
                start = Offset((size.width - lineWidth) / 2, verticalOffset),
                end = Offset((size.width + lineWidth) / 2, verticalOffset)
            )
        }
    }
}

@Composable
@Preview(device = RealisticDevices.TV_720p)
private fun NftDetailPreview() = EluvioThemePreview {
    NftDetail(
        NftDetailViewModel.State(
            loading = false,
            media = AllMediaProvider.Media(
                key = "key",
                contractAddress = "contract_address",
                versionHash = "hq__laskdjflkj322k3j4hk23j4nh2kj",
                imageUrl = "https://x",
                title = "Single Token",
                subtitle = "Special Edition",
                description = "desc",
                tokenId = "1",
                tokenCount = 1,
                tenant = "tenant",
                propertyId = "propertyId",
            ),
            contractInfo = ContractInfoDto(
                contract = "contract_address",
                cap = 1000,
                minted = 23,
                totalSupply = 400,
                burned = 3
            ),
            lookoutUrl = "https://eluv.io"
        )
    )
}
