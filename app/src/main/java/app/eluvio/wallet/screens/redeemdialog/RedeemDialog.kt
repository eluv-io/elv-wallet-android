package app.eluvio.wallet.screens.redeemdialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import app.eluvio.wallet.app.Events
import app.eluvio.wallet.data.entities.RedeemStateEntity
import app.eluvio.wallet.data.entities.RedeemableOfferEntity
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.screens.common.EluvioLoadingSpinner
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.ShimmerImage
import app.eluvio.wallet.screens.common.requestInitialFocus
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.carousel_36
import app.eluvio.wallet.theme.label_24
import app.eluvio.wallet.theme.label_40
import app.eluvio.wallet.theme.redeemAvailableText
import app.eluvio.wallet.theme.redeemExpiredText
import app.eluvio.wallet.theme.title_62
import app.eluvio.wallet.util.compose.rememberToaster
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@MainGraph
@Destination(navArgsDelegate = RedeemDialogNavArgs::class)
@Composable
fun RedeemDialog() {
    val toaster = rememberToaster()
    hiltViewModel<RedeemDialogViewModel>().subscribeToState(
        onEvent = {
            when (it) {
                is Events.NetworkError -> toaster.toast(it.defaultMessage)
                else -> {}
            }
        },
        onState = { vm, state ->
            if (state.title.isNotEmpty()) {
                // ignore empty state
                RedeemDialog(state, onRedeemClicked = { vm.redeemOrShowOffer() })
            }
        }
    )
}

@Composable
private fun RedeemDialog(state: RedeemDialogViewModel.State, onRedeemClicked: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(Overscan.defaultPadding(excludeHorizontal = true))
                .fillMaxWidth(0.8f)
                .align(Alignment.Center)
        ) {
            // Asume poster ratio by default (2/3), but update it when image is loaded
            var aspectRatio by remember { mutableFloatStateOf(2f / 3f) }
            ShimmerImage(
                model = state.image,
                contentDescription = state.title,
                onSuccess = {
                    val drawable = it.result.drawable
                    aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                },
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .aspectRatio(aspectRatio),
            )
            Spacer(Modifier.width(35.dp))
            Column {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.title_62.copy(fontSize = 34.sp)
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    val (text, color) = when (state.fulfillmentState) {
                        RedeemableOfferEntity.FulfillmentState.EXPIRED ->
                            "OFFER EXPIRED" to MaterialTheme.colorScheme.redeemExpiredText

                        RedeemableOfferEntity.FulfillmentState.AVAILABLE,
                        RedeemableOfferEntity.FulfillmentState.UNRELEASED ->
                            "OFFER VALID" to MaterialTheme.colorScheme.redeemAvailableText

                        RedeemableOfferEntity.FulfillmentState.CLAIMED_BY_PREVIOUS_OWNER ->
                            "CLAIMED BY PREVIOUS OWNER" to MaterialTheme.colorScheme.redeemExpiredText
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.label_24,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = state.dateRange, style = MaterialTheme.typography.label_24)
                }
                Spacer(Modifier.height(12.dp))

                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.carousel_36
                )

                Spacer(Modifier.height(42.dp))
                if (state.fulfillmentState == RedeemableOfferEntity.FulfillmentState.AVAILABLE) {
                    RedeemButton(state.offerStatus, onRedeemClicked)
                }
            }
        }
    }
}

@Composable
private fun RedeemButton(
    offerStatus: RedeemStateEntity.RedeemStatus,
    onRedeemClicked: () -> Unit
) {
    val text = remember(offerStatus) {
        when (offerStatus) {
            RedeemStateEntity.RedeemStatus.REDEEMED_BY_CURRENT_USER -> "View"
            RedeemStateEntity.RedeemStatus.REDEEMING -> "Redeeming..."
            RedeemStateEntity.RedeemStatus.REDEEM_FAILED,
            RedeemStateEntity.RedeemStatus.UNREDEEMED -> "Redeem Now"

            RedeemStateEntity.RedeemStatus.REDEEMED_BY_ANOTHER_USER -> null
        }
    } ?: return
    val isRedeeming by remember(offerStatus) {
        derivedStateOf { offerStatus == RedeemStateEntity.RedeemStatus.REDEEMING }
    }
    Row {
        // Should be a Card, but TV-Card can't be disabled yet.
        Surface(
            onClick = onRedeemClicked,
            enabled = !isRedeeming,
            modifier = Modifier.requestInitialFocus(),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.label_40,
                modifier = Modifier.padding(10.dp)
            )
        }
        if (isRedeeming) {
            Spacer(Modifier.width(16.dp))
            EluvioLoadingSpinner()
        }
    }
}

@Composable
@Preview(device = Devices.TV_720p)
private fun RedeemedOfferPreview() = EluvioThemePreview {
    RedeemDialog(
        RedeemDialogViewModel.State(
            title = "Nft reward offer #1",
            image = "http://foo",
            fulfillmentState = RedeemableOfferEntity.FulfillmentState.AVAILABLE,
            dateRange = "January 1, 1970 - January 1, 2042"
        ),
        onRedeemClicked = {}
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun UnredeemedOfferPreview() = EluvioThemePreview {
    RedeemDialog(
        RedeemDialogViewModel.State(
            title = "Nft reward offer #1",
            subtitle = AnnotatedString("Very special NFT offer! Don't spend it all at once!\nand another line!"),
            image = null,
            fulfillmentState = RedeemableOfferEntity.FulfillmentState.AVAILABLE,
            dateRange = "January 1, 1970 - January 1, 2042"
        ),
        onRedeemClicked = {}
    )
}
