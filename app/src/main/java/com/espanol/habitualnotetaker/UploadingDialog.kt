package com.espanol.habitualnotetaker

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.espanol.habitualnotetaker.databinding.CustomLayoutUploadingDialogBinding

class UploadingDialog : DialogFragment() {

    private var _binding: CustomLayoutUploadingDialogBinding? = null
    private val binding get() = _binding!!
    private var dialog: AlertDialog? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        _binding = CustomLayoutUploadingDialogBinding.inflate(LayoutInflater.from(requireContext()))
        builder.setView(binding.root)
        builder.setCancelable(false)
        dialog = builder.create()
        dialog?.setCanceledOnTouchOutside(false)
        return dialog!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialog?.dismiss()
    }

    fun updateProgress(progress: Int) {
        binding.progressBar.progress = progress
    }

}