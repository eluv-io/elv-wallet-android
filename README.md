## Eluvio Media Wallet for AndroidTV

An embedded TV app for viewing collected NFTs on the Eluvio Content Fabric.

See [https://live.eluv.io/media-wallet/compatible-devices](https://live.eluv.io/media-wallet/compatible-devices)
for a complete list of devices.

### Setup
Client secrets are defined in [secrets.default.properties](secrets.default.properties). To override default values (required for full functionality):  
* External developers: either edit the file directly, or create a new file in `secrets/secrets.properties`.    
* Internal Eluvio developers: sync submodules (`git submodule update --init --recursive --remote`) and follow the instructions there to generate the secrets file.

### Persistence 
We use [Realm](https://www.mongodb.com/docs/realm/sdk/kotlin/) for persistence.
Entities that need to be persisted must:
1. Implement the `RealmObject` interface
2. Have a proper implementation of `equals`/`hashCode`.
3. Include a Dagger module that provides them into a set of all Realm classes, otherwise Realm won't be aware they exist.

A sensible toString() implementation is encouraged, since `RealmObjects` can't be data classes. 

### Navigation
A LocalNavigator is provided as a composition local to allow for navigation between destinations.
This defaults to "fullscreen/top-level" navigation. For nested navigation, provide your own implementation of Navigator/NavController.

### File template

There's a lot of boilerplate involved with creating a new Composable/ViewModel pair.
Use this [Template with multiple files](https://www.jetbrains.com/help/idea/templates-with-multiple-files.html) to generate the files for you.

```
package ${PACKAGE_NAME}

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import app.eluvio.wallet.navigation.MainGraph
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.subscribeToState
import com.ramcosta.composedestinations.annotation.Destination

@Destination<MainGraph>(navArgs = ${NAME}NavArgs::class)
@Composable
fun ${NAME}() {
    hiltViewModel<${NAME}ViewModel>().subscribeToState { vm, state ->
        ${NAME}(state)
    }
}

@Composable
private fun ${NAME}(state: ${NAME}ViewModel.State) {

}

@Composable
@Preview(device = Devices.TV_720p)
private fun ${NAME}Preview() = EluvioThemePreview {
    ${NAME}(${NAME}ViewModel.State())
}
```

And create a Child Template File for the ViewModel

```
package ${PACKAGE_NAME}

import app.eluvio.wallet.app.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ${NAME}ViewModel @Inject constructor(
) : BaseViewModel<${NAME}ViewModel.State>(State()) {
    data class State(val tmp: Int = 0)
}
```

And for the NavArgs. Not every screen will need this, but it's easier to delete when not needed, than write it out when it is.

```
package ${PACKAGE_NAME}

data class ${NAME}NavArgs(val arg1: String)
```
