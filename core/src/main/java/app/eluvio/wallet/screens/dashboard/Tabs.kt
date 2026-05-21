package app.eluvio.wallet.screens.dashboard

import kotlinx.collections.immutable.persistentListOf

enum class Tabs {
    Discover,
    MyItems,
    Profile,
    ;

    companion object {
        val NoAuthTabs = persistentListOf(Discover)
        val AuthTabs = persistentListOf(Discover, MyItems, Profile)
    }
}
