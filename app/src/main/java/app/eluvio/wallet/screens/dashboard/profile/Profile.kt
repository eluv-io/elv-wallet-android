package app.eluvio.wallet.screens.dashboard.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Checkbox
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.eluvio.wallet.data.stores.Environment
import app.eluvio.wallet.screens.common.Overscan
import app.eluvio.wallet.screens.common.TvButton
import app.eluvio.wallet.screens.common.withAlpha
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.theme.header_30
import app.eluvio.wallet.util.compose.requestInitialFocus
import app.eluvio.wallet.util.subscribeToState

@Composable
fun Profile() {
    hiltViewModel<ProfileViewModel>().subscribeToState { vm, state ->
        Profile(state, onSignOut = vm::signOut, onStatingToggled = vm::onStagingToggled)
    }
}

@Composable
private fun Profile(
    state: ProfileViewModel.State,
    onSignOut: () -> Unit,
    onStatingToggled: (Boolean) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(Overscan.defaultPadding(excludeTop = true))
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Spacer(Modifier.weight(1f)) // take up remaining space
            Header("PROFILE")
            Spacer(Modifier.height(10.dp))
            InfoField("Address: ${state.address}")
            Spacer(Modifier.height(10.dp))
            InfoField("User Id: ${state.userId}")

            Spacer(Modifier.height(20.dp))
            Header("FABRIC")
            Spacer(Modifier.height(10.dp))
            val network = state.network?.prettyEnvName?.let { stringResource(it) }
            InfoField("Network: $network")
            Spacer(Modifier.height(10.dp))
            InfoField("Fabric Node: ${state.fabricNode}")
            Spacer(Modifier.height(10.dp))
            InfoField("Authority Service: ${state.authNode}")
            Spacer(Modifier.height(10.dp))
            InfoField("Eth Service: ${state.ethNode}")

            Spacer(Modifier.weight(1f)) // take up remaining space

            StagingToggleRow(state.stagingFlag, onStatingToggled)
            Spacer(Modifier.height(10.dp))
            TvButton(
                text = "Sign Out",
                onClick = onSignOut,
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .requestInitialFocus()
            )
        }
    }
}

// Temp hack for demo
@Composable
private fun StagingToggleRow(stagingFlag: Boolean, onStatingToggled: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                // androidx.tv.material3.Checkbox doesn't actually support a "focused" state, so we
                // need to fake it.
                if (focused) Color(0xff434343) else Color(0xff232323),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(10.dp)
            .fillMaxWidth()
    ) {
        Text("Set to staging", style = MaterialTheme.typography.header_30)
        Spacer(Modifier.weight(1f))
        Checkbox(
            checked = stagingFlag,
            onCheckedChange = onStatingToggled,
            interactionSource = interactionSource,
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.header_30.withAlpha(alpha = 0.6f),
        modifier = Modifier.padding(start = 10.dp)
    )
}

@Composable
private fun InfoField(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.header_30,
        modifier = Modifier
            .background(Color(0xff232323), shape = RoundedCornerShape(4.dp))
            .padding(10.dp)
            .fillMaxWidth()
    )
}

@Composable
@Preview(device = Devices.TV_720p)
private fun ProfilePreview() = EluvioThemePreview {
    Profile(
        ProfileViewModel.State(
            address = "0x00f9f89f8f98",
            userId = "ius1f1fd2d8d82e21d",
            network = Environment.Demo,
            fabricNode = "https://host-2-2-2-2.cf.io",
            authNode = "https://host-2-2-2-2.cf.io/as",
            ethNode = "https://host-2-2-2-2.cf.io/eth",
        ),
        onSignOut = {},
        onStatingToggled = {},
    )
}
