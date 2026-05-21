package app.eluvio.wallet.util.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import app.eluvio.wallet.util.logging.Log

/**
 * Utility to check device codec capabilities and recommend the best streaming format (DASH vs HLS).
 *
 * Some devices have issues with HLS playback due to:
 * - MPEG-TS container demuxing problems
 * - Buggy hardware decoders for specific codec profiles
 * - Incomplete HEVC/H.265 support
 *
 * This class probes the device's capabilities and provides a recommendation.
 */
object FormatCapabilityChecker {

    enum class PreferredFormat {
        DASH,
        HLS,
        NO_PREFERENCE
    }

    data class CodecCapabilities(
        val supportsAvc: Boolean,
        val supportsHevc: Boolean,
        val hasHardwareAvcDecoder: Boolean,
        val hasHardwareHevcDecoder: Boolean,
        val avcMaxLevel: Int,
        val hevcMaxLevel: Int,
        val preferredFormat: PreferredFormat,
        val reason: String
    )

    private val codecCapabilities: CodecCapabilities by lazy { probeCodecCapabilities() }

    /**
     * Returns the recommended format preference based on device capabilities.
     */
    fun getPreferredFormat(): PreferredFormat = codecCapabilities.preferredFormat

    /**
     * Returns detailed codec capabilities info for debugging.
     */
    fun getCapabilities(): CodecCapabilities = codecCapabilities

    /**
     * Returns true if DASH should be preferred over HLS on this device.
     */
    fun shouldPreferDash(): Boolean = codecCapabilities.preferredFormat == PreferredFormat.DASH

    /**
     * Returns true if HLS should be preferred over DASH on this device.
     */
    fun shouldPreferHls(): Boolean = codecCapabilities.preferredFormat == PreferredFormat.HLS

    private fun probeCodecCapabilities(): CodecCapabilities {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos

        var supportsAvc = false
        var supportsHevc = false
        var hasHardwareAvcDecoder = false
        var hasHardwareHevcDecoder = false
        var avcMaxLevel = 0
        var hevcMaxLevel = 0

        for (codecInfo in codecInfos) {
            if (codecInfo.isEncoder) continue

            val isHardware = isHardwareDecoder(codecInfo)

            for (type in codecInfo.supportedTypes) {
                when {
                    type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) -> {
                        supportsAvc = true
                        if (isHardware) hasHardwareAvcDecoder = true
                        val level = getMaxLevel(codecInfo, type)
                        if (level > avcMaxLevel) avcMaxLevel = level
                    }
                    type.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) -> {
                        supportsHevc = true
                        if (isHardware) hasHardwareHevcDecoder = true
                        val level = getMaxLevel(codecInfo, type)
                        if (level > hevcMaxLevel) hevcMaxLevel = level
                    }
                }
            }
        }

        val (preferredFormat, reason) = determinePreferredFormat(
            supportsAvc = supportsAvc,
            supportsHevc = supportsHevc,
            hasHardwareAvcDecoder = hasHardwareAvcDecoder,
            hasHardwareHevcDecoder = hasHardwareHevcDecoder,
            hevcMaxLevel = hevcMaxLevel
        )

        val capabilities = CodecCapabilities(
            supportsAvc = supportsAvc,
            supportsHevc = supportsHevc,
            hasHardwareAvcDecoder = hasHardwareAvcDecoder,
            hasHardwareHevcDecoder = hasHardwareHevcDecoder,
            avcMaxLevel = avcMaxLevel,
            hevcMaxLevel = hevcMaxLevel,
            preferredFormat = preferredFormat,
            reason = reason
        )

        Log.d("FormatCapabilityChecker: $capabilities")

        return capabilities
    }

    private fun isHardwareDecoder(codecInfo: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codecInfo.isHardwareAccelerated
        } else {
            // Heuristic for older APIs: software decoders usually have "OMX.google" prefix
            // or "c2.android" prefix
            val name = codecInfo.name.lowercase()
            !name.startsWith("omx.google.") && !name.startsWith("c2.android.")
        }
    }

    private fun getMaxLevel(codecInfo: MediaCodecInfo, mimeType: String): Int {
        return try {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            capabilities.profileLevels.maxOfOrNull { it.level } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun determinePreferredFormat(
        supportsAvc: Boolean,
        supportsHevc: Boolean,
        hasHardwareAvcDecoder: Boolean,
        hasHardwareHevcDecoder: Boolean,
        hevcMaxLevel: Int
    ): Pair<PreferredFormat, String> {
        // Check for known problematic devices/chipsets for HLS
        if (isKnownProblematicHlsDevice()) {
            return PreferredFormat.DASH to "Known problematic HLS device: ${Build.MANUFACTURER} ${Build.MODEL}"
        }

        // Older devices (API < 23) often have HLS issues with MPEG-TS demuxing
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return PreferredFormat.DASH to "API level ${Build.VERSION.SDK_INT} < 23, DASH more reliable"
        }

        // If device lacks hardware HEVC decoder, prefer DASH which often has better
        // fallback to AVC transcoded streams
        if (!hasHardwareHevcDecoder && supportsHevc) {
            return PreferredFormat.DASH to "No hardware HEVC decoder, prefer DASH for AVC fallback"
        }

        // Low HEVC level support may cause issues with high-quality HLS streams
        // HEVC Level 4.1 (0x1000 = 4096) is typically needed for 1080p60 or 4K30
        if (supportsHevc && hevcMaxLevel > 0 && hevcMaxLevel < MediaCodecInfo.CodecProfileLevel.HEVCMainTierLevel41) {
            return PreferredFormat.DASH to "Limited HEVC level support ($hevcMaxLevel), prefer DASH"
        }

        // Check for MediaTek chipsets which historically have HLS issues
        if (isMediaTekDevice() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return PreferredFormat.DASH to "MediaTek device on older Android, prefer DASH"
        }

        // No specific preference - both should work fine
        return PreferredFormat.NO_PREFERENCE to "Device capabilities look good for both formats"
    }

    /**
     * Returns true for devices known to have HLS playback issues.
     * This list should be updated based on crash reports and user feedback.
     */
    private fun isKnownProblematicHlsDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()

        // Amazon Fire TV Stick (1st gen) has known HLS issues
        if (manufacturer == "amazon" && (device.contains("mantis") || model.contains("aftm"))) {
            return true
        }

        // Some older Xiaomi devices have MPEG-TS demuxing issues
        if (manufacturer == "xiaomi" && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return true
        }

        // Certain RockChip-based devices have HLS issues
        val hardware = Build.HARDWARE.lowercase()
        if (hardware.contains("rk3") && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }

        return false
    }

    private fun isMediaTekDevice(): Boolean {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        return hardware.contains("mt") || board.contains("mt") ||
                hardware.contains("mediatek") || board.contains("mediatek")
    }
}
