package example.jllarraz.com.passportreader.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.mobsandgeeks.saripaar.ValidationError
import com.mobsandgeeks.saripaar.Validator
import example.jllarraz.com.passportreader.R
import example.jllarraz.com.passportreader.databinding.FragmentSelectionBinding
import example.jllarraz.com.passportreader.ui.validators.DateRule
import example.jllarraz.com.passportreader.ui.validators.DocumentNumberRule
import io.reactivex.disposables.CompositeDisposable
import net.sf.scuba.data.Gender
import org.jmrtd.lds.icao.MRZInfo
import java.security.Security

class SelectionFragment : Fragment(R.layout.fragment_selection), Validator.ValidationListener {

    private var mValidator: Validator? = null
    private var selectionFragmentListener: SelectionFragmentListener? = null
    private var disposable = CompositeDisposable()

    private lateinit var binding: FragmentSelectionBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSelectionBinding.bind(view)

        binding.apply {
            radioButtonDataEntry.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId == R.id.radioButtonOcr) {
                    if (selectionFragmentListener != null) {
                        selectionFragmentListener!!.onMrzRequest()
                    }
                }
            }


            buttonReadNfc.setOnClickListener { validateFields() }

            mValidator = Validator(this)
            mValidator!!.setValidationListener(this@SelectionFragment)

            mValidator!!.put(documentNumber, DocumentNumberRule())
            mValidator!!.put(documentExpiration, DateRule())
            mValidator!!.put(documentDateOfBirth, DateRule())
        }
    }

    private fun validateFields() {
        try {
            binding.apply {
                mValidator!!.removeRules(documentNumber)
                mValidator!!.removeRules(documentExpiration)
                mValidator!!.removeRules(documentDateOfBirth)

                mValidator!!.put(documentNumber, DocumentNumberRule())
                mValidator!!.put(documentExpiration, DateRule())
                mValidator!!.put(documentDateOfBirth, DateRule())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mValidator!!.validate()
    }

    fun selectManualToggle() {
        binding.radioButtonDataEntry.check(R.id.radioButtonManual)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is SelectionFragmentListener) {
            selectionFragmentListener = activity
        }
    }

    override fun onDetach() {
        selectionFragmentListener = null
        super.onDetach()

    }

    override fun onDestroyView() {

        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        super.onDestroyView()
    }


    override fun onValidationSucceeded() {

        binding.apply {
            val documentNumber = documentNumber.text!!.toString()
            val dateOfBirth = documentDateOfBirth.text!!.toString()
            val documentExpiration = documentExpiration.text!!.toString()

            val mrzInfo = MRZInfo("P",
                    "ESP",
                    "DUMMY",
                    "DUMMY",
                    documentNumber,
                    "ESP",
                    dateOfBirth,
                    Gender.MALE,
                    documentExpiration,
                    "DUMMY"
            )
            if (selectionFragmentListener != null) {
                selectionFragmentListener!!.onPassportRead(mrzInfo)
            }
        }
    }

    override fun onValidationFailed(errors: List<ValidationError>) {
        for (error in errors) {
            val view = error.view
            val message = error.getCollatedErrorMessage(context)

            // Display error messages ;)
            if (view is EditText) {
                view.error = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //        Listener
    //
    ////////////////////////////////////////////////////////////////////////////////////////

    interface SelectionFragmentListener {
        fun onPassportRead(mrzInfo: MRZInfo)
        fun onMrzRequest()
    }

    companion object {
        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }
}
