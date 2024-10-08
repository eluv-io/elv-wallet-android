package app.eluvio.wallet.screens.nftdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.navigation.LocalNavigator
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.navigation.asPush
import app.eluvio.wallet.network.dto.ContractInfoDto
import app.eluvio.wallet.screens.common.DelayedFullscreenLoader
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.generateQrCodeBlocking
import app.eluvio.wallet.screens.dashboard.myitems.AllMediaProvider
import app.eluvio.wallet.screens.dashboard.myitems.MediaCard
import app.eluvio.wallet.screens.destinations.PropertyDetailDestination
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.body_32
import app.eluvio.wallet.theme.carousel_36
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
private fun MetadataTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.carousel_36.copy(
            fontWeight = FontWeight.Medium,
            lineHeight = 26.sp
        )
    )
}

private enum class NftTabs(val title: String) {
    DESCRIPTION("Description") {
        @Composable
        override fun Content(state: NftDetailViewModel.State) {
            val media = state.media ?: return
            Column {
                MetadataTitle(text = media.title)
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
        override fun Content(state: NftDetailViewModel.State) {
            Column {
                // TODO: comment out until we figure what if this makes sense on TV
//                MetadataTitle("Media URL")
//                Text("http...", Modifier.padding(bottom = 20.dp))
//
//                MetadataTitle("Image URL")
//                Text("http://..", Modifier.padding(bottom = 20.dp))

                if (state.contractInfo != null) {
                    Text(state.media?.subtitle ?: "")
                    Text("Number Minted: ${state.contractInfo.minted}")
                    Text("Number in Circulation: ${state.contractInfo.totalSupply}")
                    Text("Number Burned: ${state.contractInfo.burned}")
                    Text("Maximum Possible in Circulation: ${state.contractInfo.cap - state.contractInfo.burned}")
                    Text("Cap: ${state.contractInfo.cap}")
                }
            }
        }
    },
    CONTRACT_VERSION("Contract & Version") {
        @Composable
        override fun Content(state: NftDetailViewModel.State) {
            MetadataTitle("Contract Address")
            Text(
                state.media?.contractAddress ?: "...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            MetadataTitle("Hash")
            Text(state.media?.versionHash ?: "...", maxLines = 1, overflow = TextOverflow.Ellipsis)

            var showDialog by rememberSaveable { mutableStateOf(false) }
            if (state.lookoutQr != null) {
                if (showDialog) {
                    Dialog(
                        onDismissRequest = { showDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.8f))
                        ) {
                            Image(
                                bitmap = state.lookoutQr.asImageBitmap(),
                                contentDescription = "Eluvio Lookout link",
                                modifier = Modifier.fillMaxSize(0.45f)
                            )
                        }
                    }
                }
                TvButton(
                    "See more info on Eluvio Lookout",
                    onClick = { showDialog = true },
                    Modifier.padding(vertical = 20.dp)
                )
            }
        }
    };

    @Composable
    abstract fun Content(state: NftDetailViewModel.State)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NftMetadata(state: NftDetailViewModel.State) {
    val tabs = NftTabs.entries
    var selectedTab by rememberSaveable { mutableStateOf(tabs.first()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
                alignment = Alignment.CenterHorizontally
            ),
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
        selectedTab.Content(state)
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
            lookoutQr = generateQrCodeBlocking("https://eluv.io")
        )
    )
}
