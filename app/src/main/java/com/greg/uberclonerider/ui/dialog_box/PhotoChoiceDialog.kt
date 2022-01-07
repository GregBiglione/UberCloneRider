package com.greg.uberclonerider.ui.dialog_box

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.droidman.ktoasty.KToasty
import com.greg.uberclonerider.R
import com.greg.uberclonerider.utils.Constant.Companion.ACCESS_CAMERA
import com.greg.uberclonerider.utils.Constant.Companion.READ_STORAGE
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class PhotoChoiceDialog(private val cameraListener: CameraListener, private val galleryListener: GalleryListener): DialogFragment() {

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
            requestCameraPermission()
        }
        photoGallery.setOnClickListener {
            requestGalleryPermission()
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

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Camera permission ------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Request Dexter camera permission ----------------------------
    //----------------------------------------------------------------------------------------------

    private fun requestCameraPermission(){
        Dexter.withContext(context)
                .withPermission(ACCESS_CAMERA)
                .withListener(object: PermissionListener {
                    override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse?) {
                        takePhoto()
                    }

                    override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse?) {
                        KToasty.error(requireContext(), getString(R.string.permission_denied_part_1) + " " + permissionDeniedResponse!!.permissionName
                                + " " + getString(R.string.permission_denied_part_2),
                                Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(permissionRequest: PermissionRequest?, permissionToken: PermissionToken?) {}
                }).check()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Intent to access camera -------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun takePhoto(){
        val accessCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraPermissionLauncher.launch(accessCamera)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Handle camera result ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            val bitmapPhoto = it.data?.extras?.get("data") as Bitmap
            cameraListener.applyCameraPhoto(bitmapPhoto)
            dismiss()
        }
        else{
            KToasty.error(requireContext(), getString(R.string.camera_denied), Toast.LENGTH_SHORT).show()
        }
    }

    //-------------------------------- Camera interface --------------------------------------------

    interface CameraListener {
        fun applyCameraPhoto(bitmapPhoto: Bitmap)
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Gallery permission -----------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Request Dexter gallery permission ---------------------------
    //----------------------------------------------------------------------------------------------

    private fun requestGalleryPermission(){
        Dexter.withContext(context)
                .withPermission(READ_STORAGE)
                .withListener(object: PermissionListener{
                    override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse?) {
                        pickPhotoFromGallery()
                    }

                    override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse?) {
                        KToasty.error(requireContext(), getString(R.string.permission_denied_part_1) + " " + permissionDeniedResponse!!.permissionName
                                + " " + getString(R.string.permission_denied_part_2),
                                Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(permissionRequest: PermissionRequest?, permissionToken: PermissionToken?) {}
                }).check()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Intent to access gallery -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun pickPhotoFromGallery() {
        val accessGallery = Intent(Intent.ACTION_PICK)
        accessGallery.type ="image/*"
        galleryPermissionLauncher.launch(accessGallery)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Handle gallery result ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            val uriPhoto = it.data?.data
            galleryListener.applyGalleryPhoto(uriPhoto)
            dismiss()
        }
        else{
            KToasty.error(requireContext(), getString(R.string.gallery_denied), Toast.LENGTH_SHORT).show()
        }
    }

    //-------------------------------- Gallery interface --------------------------------------------

    interface GalleryListener {
        fun applyGalleryPhoto(uriPhoto: Uri?)
    }
}