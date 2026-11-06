package arpit.morphe.patches.carbon.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import arpit.morphe.patches.carbon.shared.Constants.COMPATIBILITY_CARBON
import java.security.MessageDigest

private const val BUNDLE_PATH = "assets/index.android.bundle"

/** Hermes bytecode version shipped by Carbon 2.77.6010. */
private const val HERMES_VERSION = 96

private val HERMES_MAGIC = byteArrayOf(
    0xC6.toByte(), 0x1F, 0xBC.toByte(), 0x03,
    0xC1.toByte(), 0x03, 0x19, 0x1F,
)

/** Property the screens read to decide whether the account is subscribed. */
private const val IS_SUBSCRIBED = "isSubscribed"

/**
 * The two `GetById "isSubscribed"` instructions that read the flag as a boolean.
 *
 * Both are verified before patching: the bytecode also contains method calls
 * (`hasActiveSubscription()`) and incidental `0x37` bytes, and rewriting those
 * would break the app instead of unlocking it.
 */
private val IS_SUBSCRIBED_SITES = listOf(0x563878, 0x563A9C)

/**
 * Unlocks Carbon Premium.
 *
 * Carbon is a React Native app: the subscription gate is JavaScript compiled to
 * Hermes bytecode inside `assets/index.android.bundle`, and the JS reads the
 * account's subscription state from local flags (`isSubscribed`,
 * `hasActiveSubscription`). The patch rewrites every `GetById` that reads one of
 * those flags into a constant `true`, so the paywall and the premium-only
 * screens see an active subscription. RevenueCat and Play Billing stay in place
 * for the store-side flows, and nothing on the server is touched.
 *
 * Identifier ids and byte offsets move with every release, so both the Hermes
 * string table and the `GetById` sites are resolved at patch time instead of
 * being hardcoded.
 */
@Suppress("unused")
val unlockPremiumPatch = rawResourcePatch(
    name = "Unlock Premium",
    description = "Unlocks Carbon Premium by forcing the app's `isSubscribed` reads to true. " +
        "Account data the server owns is not affected.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_CARBON)

    execute {
        val bundle = get(BUNDLE_PATH)
        if (!bundle.exists()) throw PatchException("$BUNDLE_PATH not found.")

        val bytes = bundle.readBytes()
        val hermes = HermesBundle(bytes)

        val stringId = hermes.stringId(IS_SUBSCRIBED)
            ?: throw PatchException("Carbon identifier '$IS_SUBSCRIBED' not found.")
        hermes.forceGetByIdTrue(IS_SUBSCRIBED_SITES, stringId)

        hermes.updateTrailingHash()
        bundle.writeBytes(bytes)
    }
}

/**
 * Reader for the Hermes bytecode container (v96) that can rewrite instructions.
 *
 * Layout follows the bytecode file format: 8-byte magic, version, 20-byte source
 * hash, sixteen 32-bit header fields, a 32-byte option/padding tail, the function
 * header table, the string-kind and identifier-hash tables, the string table,
 * its overflow table and the string storage.
 */
private class HermesBundle(private val bytes: ByteArray) {
    private val header = IntArray(HEADER_FIELD_COUNT) { bytes.readUInt32(HEADER_FIELDS_OFFSET + it * 4) }

    private val functionCount get() = header[2]
    private val stringKindCount get() = header[3]
    private val identifierCount get() = header[4]
    private val stringCount get() = header[5]
    private val overflowStringCount get() = header[6]
    private val stringStorageSize get() = header[7]

    private val stringTableOffset: Int
        get() = align(HEADER_SIZE + functionCount * SMALL_FUNCTION_HEADER_SIZE) +
            align(stringKindCount * 4) + align(identifierCount * 4)

    private val overflowTableOffset: Int get() = stringTableOffset + stringCount * 4
    private val stringStorageOffset: Int get() = overflowTableOffset + overflowStringCount * 8

    init {
        if (bytes.size < HEADER_SIZE) {
            throw PatchException("Carbon bundle is too small to be Hermes bytecode.")
        }
        for (index in HERMES_MAGIC.indices) {
            if (bytes[index] != HERMES_MAGIC[index]) {
                throw PatchException("Carbon bundle is not Hermes bytecode.")
            }
        }
        val version = bytes.readUInt32(8)
        if (version != HERMES_VERSION) {
            throw PatchException("Unsupported Carbon Hermes bytecode version: $version")
        }
        if (header[0] != bytes.size) {
            throw PatchException("Carbon bundle length mismatch: ${header[0]} != ${bytes.size}")
        }
    }

    /** Resolves an ASCII string-table entry to its bytecode string id. */
    fun stringId(value: String): Int? {
        val target = value.toByteArray(Charsets.UTF_8)
        for (index in 0 until stringCount) {
            val raw = bytes.readUInt32(stringTableOffset + index * 4)
            val isUtf16 = raw and 1
            val offset = (raw ushr 1) and SMALL_STRING_OFFSET_MASK
            val length = (raw ushr 24) and 0xFF
            if (isUtf16 != 0 || length == OVERFLOW_LENGTH || offset == OVERFLOW_OFFSET) continue
            if (length != target.size || offset + length > stringStorageSize) continue

            var matches = true
            for (position in target.indices) {
                if (bytes[stringStorageOffset + offset + position] != target[position]) {
                    matches = false
                    break
                }
            }
            if (matches) return index
        }
        return null
    }

    /**
     * Replaces the `GetById` at each verified offset by a constant `true`.
     *
     * `GetById` is six bytes (opcode, destination register, object register,
     * cache index, 16-bit string id); three two-byte `LoadConstTrue` instructions
     * fill the same space and leave the destination register set.
     */
    fun forceGetByIdTrue(sites: List<Int>, stringId: Int) {
        for (site in sites) {
            if (site < 0 || site + 6 > bytes.size) {
                throw PatchException("Carbon gate at 0x${site.toString(16)} is out of range.")
            }
            if ((bytes[site].toInt() and 0xFF) != GET_BY_ID) {
                throw PatchException("Carbon gate at 0x${site.toString(16)} is not a GetById.")
            }

            val candidate = (bytes[site + 4].toInt() and 0xFF) or
                ((bytes[site + 5].toInt() and 0xFF) shl 8)
            if (candidate != stringId) {
                throw PatchException("Carbon gate at 0x${site.toString(16)} reads the wrong property.")
            }

            val destination = bytes[site + 1]
            bytes[site] = LOAD_CONST_TRUE.toByte()
            bytes[site + 1] = destination
            bytes[site + 2] = LOAD_CONST_TRUE.toByte()
            bytes[site + 3] = destination
            bytes[site + 4] = LOAD_CONST_TRUE.toByte()
            bytes[site + 5] = destination
        }
    }

    /** Refreshes the SHA-1 trailer Hermes keeps over the bundle body. */
    fun updateTrailingHash() {
        val content = bytes.copyOfRange(0, bytes.size - HASH_SIZE)
        MessageDigest.getInstance("SHA-1").digest(content).copyInto(bytes, bytes.size - HASH_SIZE)
    }

    private companion object {
        const val HEADER_FIELDS_OFFSET = 32
        const val HEADER_FIELD_COUNT = 16
        const val HEADER_SIZE = 128
        const val SMALL_FUNCTION_HEADER_SIZE = 16
        const val SMALL_STRING_OFFSET_MASK = 0x7FFFFF
        const val OVERFLOW_OFFSET = 0x7FFFFF
        const val OVERFLOW_LENGTH = 0xFF
        const val HASH_SIZE = 20
        const val GET_BY_ID = 0x37
        const val LOAD_CONST_TRUE = 0x78
    }
}

private fun align(value: Int): Int = (value + 3) and 3.inv()

private fun ByteArray.readUInt32(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or
        ((this[offset + 1].toInt() and 0xFF) shl 8) or
        ((this[offset + 2].toInt() and 0xFF) shl 16) or
        ((this[offset + 3].toInt() and 0xFF) shl 24)
