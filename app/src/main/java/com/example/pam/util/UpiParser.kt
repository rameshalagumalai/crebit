package com.example.pam.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLDecoder
import java.net.URLEncoder

data class ScannedUpiData(
    val upiId: String,
    val payeeName: String? = null,
    val amount: Double? = null,
    val note: String? = null,
    val refId: String? = null,
    val rawUri: String,
    val rawParams: Map<String, String> = emptyMap()
)

sealed class UpiLaunchResult {
    data object Success : UpiLaunchResult()
    data object NoAppFound : UpiLaunchResult()
    data class Error(val message: String) : UpiLaunchResult()
}

object UpiParser {

    private val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$")

    /**
     * Parses a string (QR code content or raw text) into ScannedUpiData.
     * Supports both full upi://pay?... URIs and plain UPI IDs (e.g. name@bank).
     */
    fun parse(content: String): ScannedUpiData? {
        val trimmed = content.trim()

        if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
            return parseUpiUri(trimmed)
        }

        // Check if it's a raw UPI ID (e.g., example@oksbi, phone@upi)
        if (isValidUpiId(trimmed)) {
            return ScannedUpiData(
                upiId = trimmed,
                rawUri = buildUpiUriString(upiId = trimmed, payeeName = null, amount = null, note = null)
            )
        }

        return null
    }

    private fun parseUpiUri(uriString: String): ScannedUpiData? {
        return try {
            val qIndex = uriString.indexOf('?')
            if (qIndex == -1) return null
            val queryString = uriString.substring(qIndex + 1)
            val paramMap = mutableMapOf<String, String>()

            queryString.split("&").forEach { pair ->
                val eqIndex = pair.indexOf('=')
                if (eqIndex != -1) {
                    val key = pair.substring(0, eqIndex)
                    val rawValue = pair.substring(eqIndex + 1)
                    val value = if (key.equals("sign", ignoreCase = true)) {
                        // Do not decode 'sign' with URLDecoder as '+' in Base64 will turn into space ' '
                        rawValue
                    } else {
                        decode(rawValue)
                    }
                    paramMap[key] = value
                }
            }

            val pa = paramMap["pa"] ?: return null
            val pn = paramMap["pn"]
            val amStr = paramMap["am"]
            val amount = amStr?.toDoubleOrNull()
            val tn = paramMap["tn"]
            val tr = paramMap["tr"]

            ScannedUpiData(
                upiId = pa,
                payeeName = pn,
                amount = amount,
                note = tn,
                refId = tr,
                rawUri = uriString,
                rawParams = paramMap
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isValidUpiId(text: String): Boolean {
        return upiRegex.matches(text)
    }

    /**
     * Resolves the final UPI URI to launch.
     * If an authentic scanned URI is available and the UPI ID was not altered by the user,
     * it preserves the raw URI with all merchant signatures, orgid, and merchant tags,
     * updating or appending only the amount parameter.
     */
    fun getPaymentUri(
        rawScannedUri: String?,
        upiId: String,
        payeeName: String?,
        amount: Double?,
        note: String?,
        refId: String? = null,
        rawParams: Map<String, String> = emptyMap()
    ): String {
        val trimmedUpiId = upiId.trim()
        val parsedFromRaw = if (!rawScannedUri.isNullOrBlank()) parse(rawScannedUri) else null

        // If the user hasn't changed the UPI ID from the scanned QR, use the authentic raw URI
        if (!rawScannedUri.isNullOrBlank() && parsedFromRaw != null &&
            parsedFromRaw.upiId.equals(trimmedUpiId, ignoreCase = true) &&
            rawScannedUri.startsWith("upi://pay", ignoreCase = true)
        ) {
            return updateScannedUriAmount(rawScannedUri, amount)
        }

        return buildUpiUriString(
            upiId = trimmedUpiId,
            payeeName = payeeName,
            amount = amount,
            note = note,
            refId = refId,
            rawParams = rawParams
        )
    }

    private fun updateScannedUriAmount(rawUri: String, amount: Double?): String {
        // Ensure 'pa' in rawUri uses literal '@' instead of '%40' as required by NPCI/GPay
        val paPattern = Regex("([?&]pa=)([^&]+)", RegexOption.IGNORE_CASE)
        var uri = paPattern.replace(rawUri) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val vpaValue = matchResult.groupValues[2].replace("%40", "@")
            "$prefix$vpaValue"
        }

        if (amount != null && amount > 0.0) {
            val formattedAm = String.format(java.util.Locale.US, "%.2f", amount)
            val amPattern = Regex("([?&]am=)([^&]*)", RegexOption.IGNORE_CASE)
            uri = if (amPattern.containsMatchIn(uri)) {
                amPattern.replace(uri) { match ->
                    "${match.groupValues[1]}$formattedAm"
                }
            } else {
                val sep = if (uri.contains("?")) "&" else "?"
                "$uri${sep}am=$formattedAm"
            }
        }

        // Ensure currency code 'cu=INR' is present if missing
        if (!Regex("[?&]cu=[^&]*", RegexOption.IGNORE_CASE).containsMatchIn(uri)) {
            val sep = if (uri.contains("?")) "&" else "?"
            uri = "$uri${sep}cu=INR"
        }

        return uri
    }

    fun buildUpiUriString(
        upiId: String,
        payeeName: String?,
        amount: Double?,
        note: String?,
        refId: String? = null,
        rawParams: Map<String, String> = emptyMap()
    ): String {
        // Preserve all original parameters (e.g. mc, mode, orgid, tid, sign, etc.)
        val finalParams = LinkedHashMap<String, String>(rawParams)

        finalParams["pa"] = upiId
        if (!payeeName.isNullOrBlank()) {
            finalParams["pn"] = payeeName
        }
        if (amount != null && amount > 0.0) {
            finalParams["am"] = String.format(java.util.Locale.US, "%.2f", amount)
        }
        if (!finalParams.containsKey("cu")) {
            finalParams["cu"] = "INR"
        }
        if (!note.isNullOrBlank()) {
            finalParams["tn"] = note
        }
        if (!refId.isNullOrBlank()) {
            finalParams["tr"] = refId
        }

        val builder = StringBuilder("upi://pay?")
        val paramList = mutableListOf<String>()
        for ((key, value) in finalParams) {
            if (value.isNotBlank()) {
                val encodedVal = when {
                    key.equals("pa", ignoreCase = true) -> {
                        // VPA/UPI ID must have literal '@' per RFC 3986 and NPCI specifications.
                        // URL-encoding '@' to '%40' breaks UPI ID lookup in Google Pay / NPCI switch.
                        encode(value).replace("%40", "@")
                    }
                    key.equals("sign", ignoreCase = true) -> {
                        // Keep cryptographic signature intact
                        value
                    }
                    else -> encode(value)
                }
                paramList.add("${key}=${encodedVal}")
            }
        }
        builder.append(paramList.joinToString("&"))
        return builder.toString()
    }

    fun launchUpiPayment(context: Context, upiUriString: String): UpiLaunchResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(upiUriString)
            }
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Verify if there are apps to handle the intent
            val packageManager = context.packageManager
            val resolvedActivities = packageManager.queryIntentActivities(intent, 0)

            if (resolvedActivities.isNotEmpty()) {
                context.startActivity(chooser)
                UpiLaunchResult.Success
            } else {
                // Try launching anyway or fallback
                try {
                    context.startActivity(chooser)
                    UpiLaunchResult.Success
                } catch (e: Exception) {
                    UpiLaunchResult.NoAppFound
                }
            }
        } catch (e: Exception) {
            UpiLaunchResult.Error(e.localizedMessage ?: "Failed to open UPI app")
        }
    }

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        } catch (e: Exception) {
            value
        }
    }

    private fun decode(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }
}
