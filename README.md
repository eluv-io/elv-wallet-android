## Eluvio Media Wallet for AndroidTV

An embedded TV app for streaming media from the Content Fabric.

See [https://live.eluv.io/media-wallet/compatible-devices](https://live.eluv.io/media-wallet/compatible-devices)
for a complete list of devices.

### Setup
Client secrets are defined in [secrets.default.properties](secrets.default.properties). To override default values (required for full functionality):
* External developers: either edit the file directly, or create a new file in `secrets/secrets.properties`. Firebase will be disabled in your build (the google-services.json file is gitignored).
* Internal Eluvio developers: run `bin/fetch-secrets.sh` to populate `secrets/secrets.properties` and `app/google-services.json`.

### Persistence 
We use [Realm](https://www.mongodb.com/docs/realm/sdk/kotlin/) for persistence.
Entities that need to be persisted must:
1. Implement the `RealmObject` interface
2. Have a proper implementation of `equals`/`hashCode`.
3. Include a Dagger module that provides them into a set of all Realm classes, otherwise Realm won't be aware they exist.

A sensible toString() implementation is encouraged, since `RealmObjects` can't be data classes. 

### Navigation

The app uses [Jetpack Navigation 3](https://developer.android.com/guide/navigation/navigation-3). The host
is `MainNavHost` in `:tv`, which renders a `NavDisplay` over a `NavBackStack<NavKey>` and resolves
entries through the [nav3-hilt-vm](https://github.com/stavfx/nav3-hilt-vm) library — a small KSP
processor that generates the Hilt assisted-injection scaffolding from a `@HiltNavArgViewModel`
annotation.

For each screen with a `NavKey` + ViewModel, the library generates a `<vm>Entry { vm -> Screen(vm) }`
extension that wires `hiltViewModel<>(creationCallback = …)`. The host calls these directly.

A `LocalNavigator` is provided as a CompositionLocal for in-app navigation events
(`navigator(NavigationEvent.Push(…))`). It mutates the backstack list — no `NavController`.

### File template

There's a lot of boilerplate involved with creating a new Composable/ViewModel pair.
Use this [Template with multiple files](https://www.jetbrains.com/help/idea/templates-with-multiple-files.html) to generate the files for you.

Register the screen in `MainNavHost` with `${name}Entry { vm -> ${NAME}(vm) }` (the entry helper
is auto-generated from the `@HiltNavArgViewModel`-annotated VM in the same package).

```
package ${PACKAGE_NAME}

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import app.eluvio.wallet.theme.EluvioThemePreview
import app.eluvio.wallet.util.subscribeToState

@Composable
fun ${NAME}(vm: ${NAME}ViewModel) {
    vm.subscribeToState { _, state ->
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

And create a Child Template File for the ViewModel. `@HiltNavArgViewModel` triggers codegen of the
Hilt subclass + entry helper; `@NavArg` marks the constructor parameter that carries the route key.
The class must be `open`.

```
package ${PACKAGE_NAME}

import app.eluvio.wallet.app.BaseViewModel
import com.stavfx.nav3hiltvm.annotations.HiltNavArgViewModel
import com.stavfx.nav3hiltvm.annotations.NavArg

@HiltNavArgViewModel
open class ${NAME}ViewModel(
    @NavArg private val navArgs: ${NAME}NavArgs,
) : BaseViewModel<${NAME}ViewModel.State>(State()) {
    data class State(val tmp: Int = 0)
}
```

And the NavArgs. `@Serializable` is required so the route can be persisted in the back stack; the
class must implement `NavKey` so Nav 3 accepts it as a back-stack entry. No `typeMap` entries
needed — kotlinx-serialization handles nested types natively now.

```
package ${PACKAGE_NAME}

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ${NAME}NavArgs(val arg1: String) : NavKey
```

For screens without nav args (rare — `Dashboard` is the main example), skip `@HiltNavArgViewModel` /
`@NavArg`, declare a `@Serializable data object FooNavArgs : NavKey` for the route, use plain
`@HiltViewModel` / `@Inject` on the VM, and register with raw `entry<FooNavArgs> { Foo() }` in
`MainNavHost`.
