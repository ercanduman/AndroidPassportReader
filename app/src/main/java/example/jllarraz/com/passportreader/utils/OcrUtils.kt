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

    private const val REGEX_ID_CARD_DOC_NUMBER = "([A|C|I]<[A-Z0-9]{1})([A-Z]{3})([A-Z0-9<]{9}<)"
    private const val REGEX_ID_CARD_DATES = "([0-9]{6})([0-9])([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z]{3})([A-Z0-9<]{11})([0-9])"

    private const val REGEX_PASSPORT_DOC_NUMBER = "(P[A-Z0-9<]{1})([A-Z]{3})([A-Z0-9<]{39})"
    private const val REGEX_PASSPORT_DATES = "([A-Z0-9<]{9})([0-9]{1})([A-Z]{3})([0-9]{6})([0-9]{1})([M|F|X|<]{1})([0-9]{6})([0-9]{1})([A-Z0-9<]{14})([0-9]{1})([0-9]{1})"

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
            fullRead.indexOf(TYPE_ID_CARD) > 0 -> { // Read ID card
                val matcherIdCard = Pattern.compile(ID_CARD_TD_1_LINE_1_REGEX).matcher(fullRead)
                val matcherIdCardLine2 = Pattern.compile(ID_CARD_TD_1_LINE_2_REGEX).matcher(fullRead)
                if (!matcherIdCard.find() || !matcherIdCardLine2.find()) {
                    callback.onMRZReadFailure(timeRequired)
                    return
                }
                val line2: String = matcherIdCardLine2.group(0) ?: ""

                fullRead = fullRead.substring(fullRead.indexOf(TYPE_ID_CARD))
                var documentNumber: String = fullRead.substring(5, 14)
                documentNumber = documentNumber.replace("O", "0")
                val dateOfBirthDay: String = line2.substring(0, 6)
                val expiryDate: String = line2.substring(8, 14)
                Log.d(TAG, "Scanned Text Buffer ID Card ->>>> Doc Number: $documentNumber DateOfBirth: $dateOfBirthDay ExpiryDate: $expiryDate")

                val mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expiryDate)
                callback.onMRZRead(mrzInfo, timeRequired)

            }
            fullRead.indexOf(TYPE_PASSPORT) > 0 -> { // Read Passport

                val matcherPassport = Pattern.compile(PASSPORT_TD_3_LINE_1_REGEX).matcher(fullRead)
                val matcherPassportLine2 = Pattern.compile(PASSPORT_TD_3_LINE_2_REGEX).matcher(fullRead)
                if (matcherPassport.find().not() || matcherPassportLine2.find().not()) {
                    callback.onMRZReadFailure(timeRequired)
                    return
                }
                val line2: String = matcherPassportLine2.group(0) ?: ""

                val mrzInfo = createDummyMrz(documentNumber, dateOfBirthDay, expirationDate)
                callback.onMRZRead(mrzInfo, timeRequired)
            }
            else -> { //No success
                callback.onMRZReadFailure(timeRequired)
            }
        }
    }

    private fun createDummyMrz(documentNumber: String, dateOfBirthDay: String, expirationDate: String): MRZInfo {
        return MRZInfo(
                documentCode,
                NOT_APPLICABLE,
                NOT_APPLICABLE,
                NOT_APPLICABLE,
                documentNumber,
                NOT_APPLICABLE,
                cleanDate(dateOfBirthDay),
                Gender.UNSPECIFIED,
                cleanDate(expirationDate),
                NOT_APPLICABLE
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

    interface MRZCallback {
        fun onMRZRead(mrzInfo: MRZInfo, timeRequired: Long)
        fun onMRZReadFailure(timeRequired: Long)
        fun onFailure(e: Exception, timeRequired: Long)
    }
}