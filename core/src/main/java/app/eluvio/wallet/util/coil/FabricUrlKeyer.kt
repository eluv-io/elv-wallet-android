package app.eluvio.wallet.util.coil

import coil3.Uri
import coil3.key.Keyer
import coil3.request.Options

/**
 * Coil [Keyer] that normalizes contentfabric.io URLs by stripping the rotating `host-x-x-x-x.`
 * subdomain. The fabric serves identical content from multiple nodes, and the config rotates
 * which one we use — without this, the same image gets a fresh cache entry each rotation.
 *
 * `FabricUrl`-typed data is already keyed by path (see FabricUrlEntity.equals); this only
 * matters for places that pass a raw URL string to Coil. Note: Coil maps `String → Uri` before
 * keyers run, so this is a `Keyer<Uri>` rather than `Keyer<String>`.
 *
 * Non-fabric URLs return null so Coil falls back to default keying.
 */
class FabricUrlKeyer : Keyer<Uri> {
    override fun key(data: Uri, options: Options): String? {
        val host = data.authority ?: return null
        if (!host.endsWith(".contentfabric.io") || !host.startsWith("host-")) return null
        return data.toString().replace(host, "contentfabric.io")
    }
}
