package com.greg.uberclonerider

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.droidman.ktoasty.KToasty

class PhotoChoiceDialog: DialogFragment() {

    private lateinit var photoCamera: ImageView
    private lateinit var photoGallery: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context)
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.dialog_photo_choice, null)
        builder.setView(view)

        photoCamera = view.findViewById(R.id.dialog_photo_camera)
        photoGallery = view.findViewById(R.id.dialog_photo_gallery)
        photoCamera.setOnClickListener {
            KToasty.success(requireContext(), "Camera", Toast.LENGTH_SHORT).show()
        }
        photoGallery.setOnClickListener {
            KToasty.warning(requireContext(), "Gallery", Toast.LENGTH_SHORT).show()
        }

        builder.setTitle(R.string.choose_photo)
                .setNegativeButton(R.string.cancel){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
        return dialog
    }
}