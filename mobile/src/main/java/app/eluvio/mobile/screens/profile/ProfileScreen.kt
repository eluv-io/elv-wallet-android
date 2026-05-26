package app.eluvio.mobile.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eluvio.mobile.R
import app.eluvio.wallet.screens.dashboard.profile.ProfileViewModel

@Composable
fun ProfileScreen(
    state: ProfileViewModel.State,
    onSignOut: () -> Unit,
    onNetworkRowTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loggedIn = state.address.isNotBlank()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(loggedIn = loggedIn)
        Text(
            text = state.email ?: state.address,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
        ) {
            val rows = buildList {
                if (loggedIn && state.email != null) {
                    add(LabelValueRow(R.string.profile_label_email, state.email!!))
                }
                if (loggedIn) {
                    add(LabelValueRow(R.string.profile_label_user_id, state.userId, monospace = true))
                    add(LabelValueRow(R.string.profile_label_address, state.address, monospace = true, valueSmall = true))
                    add(LabelValueRow(R.string.profile_label_session, state.sessionExpiration.orEmpty()))
                }
                add(LabelValueRow(R.string.profile_label_network, state.network?.name.orEmpty(), onClick = onNetworkRowTap))
                add(LabelValueRow(R.string.profile_label_version, state.appVersion))
            }
            rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                LabelValueRowView(row)
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.profile_sign_out))
        }
    }
}

@Composable
private fun Avatar(loggedIn: Boolean) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .alpha(if (loggedIn) 1f else 0f) // invisible (not gone) when not logged in
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp),
        )
    }
}

private data class LabelValueRow(
    val labelRes: Int,
    val value: String,
    val monospace: Boolean = false,
    val valueSmall: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun LabelValueRowView(row: LabelValueRow) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (row.onClick != null) it.clickable(onClick = row.onClick) else it }
        .padding(16.dp)
    Column(rowModifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(row.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            row.value,
            style = if (row.valueSmall) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontFamily = if (row.monospace) FontFamily.Monospace else null,
        )
    }
}
