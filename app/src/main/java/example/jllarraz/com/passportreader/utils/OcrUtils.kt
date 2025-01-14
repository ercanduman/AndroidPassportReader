package example.jllarraz.com.passportreader.utils

import android.util.Log
import com.google.mlkit.vision.text.Text
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.MRZInfo
import java.util.regex.Pattern

object OcrUtils {
    private const val TAG = "OcrUtils"

    private const val TYPE_PASSPORT = "P<"
    private const val TYPE_ID_CARD = "I<"
    private const val NOT_APPLICABLE = "N/A"
    private const val MRZ_DOCUMENT_CODE = "P"

    /**
     * For testing REGEX following link can be visited. https://www.regextester.com/99458
     */
    private const val REGEX_ID_CARD_DOC_NUMBER = "([A|C|I]<[A-Z0-9])([A-Z]{3})([A-Z0-9<]{9}<)"
    private const val REGEX_ID_CARD_DATES = "([0-9]{6})([0-9])([M|F|X|<])([0-9]{6})([0-9])([A-Z]{3})([A-Z0-9<]{11})([0-9])"

    private const val REGEX_PASSPORT_LINE_1 = "(P<[A-Z0-9])([A-Z])([A-Z0-9<]{30})"
    private const val REGEX_PASSPORT_LINE_2 = "([A-Z0-9<]{9})([0-9])([A-Z]{3})([0-9]{6})([0-9])([M|F|X|<])([0-9]{6})([0-9])([A-Z0-9<]{14})([0-9])([0-9])"

    fun processOcr(results: Text, timeRequired: Long, callback: MRZCallback) {
        var fullRead = ""
        val blocks = results.textBlocks
        for (i in blocks.indices) {
            var temp = ""
            val lines = blocks[i].lines
            for (j in lines.indices) {
                //extract scanned text lines here
                //temp+=lines.get(j).getText().trim()+"-";
                temp += lines[j].text + "-"
            }
            temp = temp.replace("\r".toRegex(), "").replace("\n".toRegex(), "").replace("\t".toRegex(), "").replace(" ", "")
            fullRead += "$temp-"
        }
        fullRead = fullRead.replace("--", "").uppercase()
        Log.d(TAG, "Read: $fullRead")

        when {
            isIdCard(fullRead) -> readIdCardText(fullRead, callback, timeRequired)
            isPassport(fullRead) -> readPassportText(fullRead, callback, timeRequired)
            else -> callback.onMRZReadFailure(timeRequired) //No success
        }
    }

    private fun isIdCard(fullRead: String) = fullRead.indexOf(TYPE_ID_CARD) > 0

    private fun isPassport(fullRead: String) = (fullRead.indexOf(TYPE_PASSPORT) > 0
            || Pattern.compile(REGEX_PASSPORT_LINE_2).matcher(fullRead).find())

    private fun readIdCardText(fullRead: String, callback: MRZCallback, timeRequired: Long) {
        var idCardText = fullRead
        val matcherIdCardDocNumber = Pattern.compile(REGEX_ID_CARD_DOC_NUMBER).matcher(idCardText)
        val matcherIdCardDates = Pattern.compile(REGEX_ID_CARD_DATES).matcher(idCardText)
        if (!matcherIdCardDocNumber.find() || !matcherIdCardDates.find()) {
            callback.onMRZReadFailure(timeRequired)
            return
        }

        idCardText = idCardText.substring(idCardText.indexOf(TYPE_ID_CARD))
        var documentNumber: String = idCardText.substring(5, 14)
        documentNumber = documentNumber.replace("O", "0")

        val dates: String = matcherIdCardDates.group(0) ?: ""
        var dateOfBirth: String = dates.substring(0, 6)
        var dateOfExpiry: String = dates.substring(8, 14)
        Log.d(TAG, "Scanned Text Buffer ID Card ->>>> Doc Number: $documentNumber DateOfBirth: $dateOfBirth DateOfExpiry: $dateOfExpiry")
        dateOfBirth = isDateValid(dateOfBirth, callback, timeRequired)
        dateOfExpiry = isDateValid(dateOfExpiry, callback, timeRequired)
        Log.d(TAG, "Scanned Text Buffer CLEANED Dates ->>>> DateOfBirth: $dateOfBirth DateOfExpiry: $dateOfExpiry")

        val mrzInfo = createDummyMrz(documentNumber, dateOfBirth, dateOfExpiry)
        if (isMrzValid(mrzInfo)) {
            callback.onMRZRead(mrzInfo, timeRequired)
        } else {
            callback.onMRZReadFailure(timeRequired)
        }
    }

    private fun readPassportText(fullRead: String, callback: MRZCallback, timeRequired: Long) {
        val matcherPassportDates = Pattern.compile(REGEX_PASSPORT_LINE_2).matcher(fullRead)
        if (matcherPassportDates.find().not()) {
            callback.onMRZReadFailure(timeRequired)
            return
        }

        val dates: String = matcherPassportDates.group(0) ?: ""
        var documentNumber = dates.substring(0, 9)
        documentNumber = documentNumber.replace("O", "0")
        var dateOfBirth = dates.substring(13, 19)
        var dateOfExpiry = dates.substring(21, 27)
        Log.d(TAG, "Scanned Text Buffer ID Card ->>>> Doc Number: $documentNumber DateOfBirth: $dateOfBirth DateOfExpiry: $dateOfExpiry")
        dateOfBirth = isDateValid(dateOfBirth, callback, timeRequired)
        dateOfExpiry = isDateValid(dateOfExpiry, callback, timeRequired)
        Log.d(TAG, "Scanned Text Buffer CLEANED Dates ->>>> DateOfBirth: $dateOfBirth DateOfExpiry: $dateOfExpiry")

        val mrzInfo = createDummyMrz(documentNumber, dateOfBirth, dateOfExpiry)
        if (isMrzValid(mrzInfo)) {
            Log.d(TAG, "readPassportText: Valid MRZ")
            callback.onMRZRead(mrzInfo, timeRequired)
        } else {
            callback.onMRZReadFailure(timeRequired)
        }
    }

    private fun isDateValid(dateOfExpiry: String, callback: MRZCallback, timeRequired: Long): String {
        var validDate = dateOfExpiry
        try {
            validDate = cleanDate(validDate)
            validDate.toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            callback.onMRZReadFailure(timeRequired)
        }
        return validDate
    }

    private fun createDummyMrz(documentNumber: String, dateOfBirth: String, dateOfExpiry: String): MRZInfo {
        return MRZInfo(
                MRZ_DOCUMENT_CODE,
                NOT_APPLICABLE,
                NOT_APPLICABLE,
                NOT_APPLICABLE,
                documentNumber,
                NOT_APPLICABLE,
                dateOfBirth,
                Gender.UNSPECIFIED,
                dateOfExpiry,
                ""
        )
    }

    private fun cleanDate(date: String): String {
        var tempDate = date
        tempDate = tempDate.replace("I".toRegex(), "1")
        tempDate = tempDate.replace("L".toRegex(), "1")
        tempDate = tempDate.replace("D".toRegex(), "0")
        tempDate = tempDate.replace("O".toRegex(), "0")
        tempDate = tempDate.replace("S".toRegex(), "5")
        tempDate = tempDate.replace("G".toRegex(), "6")
        return tempDate
    }

    private fun isMrzValid(mrzInfo: MRZInfo): Boolean {
        return mrzInfo.documentNumber != null && mrzInfo.documentNumber.length >= 9
                && mrzInfo.dateOfBirth != null && mrzInfo.dateOfBirth.length == 6
                && mrzInfo.dateOfExpiry != null && mrzInfo.dateOfExpiry.length == 6
    }

    interface MRZCallback {
        fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long)
        fun onMRZReadFailure(timeRequired: Long)
        fun onFailure(e: Exception, timeRequired: Long)
    }
}