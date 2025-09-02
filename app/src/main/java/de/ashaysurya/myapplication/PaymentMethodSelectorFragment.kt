// File: PaymentMethodSelectorFragment.kt
package de.ashaysurya.myapplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.ashaysurya.myapplication.databinding.FragmentPaymentSelectorBinding

class PaymentMethodSelectorFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPaymentSelectorBinding? = null
    private val binding get() = _binding!!

    private var listener: PaymentMethodSelectionListener? = null

    interface PaymentMethodSelectionListener {
        fun onPaymentMethodSelected(method: PaymentMethod)
    }

    // THIS IS THE CORRECTED FUNCTION
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This is the correct way to get the listener from a NavHostFragment setup
        listener = parentFragmentManager.primaryNavigationFragment as? PaymentMethodSelectionListener

        if (listener == null) {
            throw RuntimeException("$context must have a primary navigation fragment that implements PaymentMethodSelectionListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionCash.setOnClickListener {
            listener?.onPaymentMethodSelected(PaymentMethod.CASH)
            dismiss()
        }

        binding.optionCredit.setOnClickListener {
            listener?.onPaymentMethodSelected(PaymentMethod.CREDIT)
            dismiss()
        }

        binding.optionUpi.setOnClickListener {
            listener?.onPaymentMethodSelected(PaymentMethod.UPI)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }



    companion object {
        const val TAG = "PaymentMethodSelectorFragment"
        fun newInstance(): PaymentMethodSelectorFragment {
            return PaymentMethodSelectorFragment()
        }
    }
}