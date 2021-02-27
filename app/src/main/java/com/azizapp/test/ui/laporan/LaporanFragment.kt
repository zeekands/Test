package com.azizapp.test.ui.laporan

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.azizapp.test.R
import com.azizapp.test.api.MyAPI
import com.azizapp.test.databinding.FragmentLaporanBinding
import com.azizapp.test.model.DataPengaduanMasyarakat
import com.azizapp.test.utill.Session
import com.azizapp.test.utill.getFileName
import com.azizapp.test.utill.snackbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.bottom_sheet_dialog.view.*
import kotlinx.android.synthetic.main.fragment_laporan.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject


@AndroidEntryPoint
class LaporanFragment @Inject constructor(val typeUser : String) : Fragment() {

    lateinit var binding: FragmentLaporanBinding
    private val laporanViewModel: LaporanViewModel by viewModels()

    var imageUri: Uri? = null
var sImage: String?=null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        pilihLaporan()
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_laporan, container, false)
        //val inflater = inflater.inflate(R.layout.fragment_laporan, container, false)
        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModelLaporan = laporanViewModel
        }
        binding.editTextNamaJalan.setOnClickListener {
            val intent = Intent(activity, LaporanActivity::class.java)
            startActivityForResult(intent, 100)
        }
        laporanViewModel.action.observe(this.viewLifecycleOwner, Observer { action ->
            when (action) {
                LaporanViewModel.ACTION_SUCCESS -> actionSuccess()
                LaporanViewModel.ACTION_ERROR -> actionError()
                LaporanViewModel.ACTION_FAILED -> actionFailed()
            }

        })
        binding.editGambar.setOnClickListener {
            Intent(Intent.ACTION_PICK).also {
                it.type = "image/*"
                val mimeTypes = arrayOf("image/jpeg", "image/png")
                it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                startActivityForResult(it, 1)
            }
        }
        binding.buttonLapor.setOnClickListener() {
           when(typeUser){
               "login" -> uploadImage()
               "anonim" -> uploadImageAnonym()
           }
        }

        return binding.root
    }



    private fun actionFailed() {
        Snackbar.make(binding.root, "Action Failed", Snackbar.LENGTH_SHORT).show()
    }

    private fun actionError() {
        Snackbar.make(binding.root, "Action Error", Snackbar.LENGTH_SHORT).show()
    }

    private fun actionSuccess() {
        val intent = Intent(activity, SuccessPage::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val lat = data.getDoubleExtra("LAT", 0.0)
            val long = data.getDoubleExtra("LONG", 0.0)
            editTextNamaJalan.setText(data.getStringExtra("ADDRESS"))
            editTextLokasi.setText("[$lat,$long]")
        } else if (requestCode == 1 && data != null) {
            imageUri = data.data
            editGambar.setImageURI(imageUri)
            val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,imageUri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream)

            val bytes = byteArrayOutputStream.toByteArray()
            sImage = Base64.encodeToString(bytes,Base64.DEFAULT)

        }
    }


    fun pilihLaporan(): String {
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        val dialog = BottomSheetDialog(requireActivity())
        var jenis: String = ""
        dialog.setContentView(view)
        dialog.show()

        view.titik_tersumbat.setOnClickListener {
            jenis = "Titik Tersumbat"
            Toast.makeText(
                activity,
                "Anda melaporkan $jenis", Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
            tv_laporkan.text = "Laporkan $jenis"
        }

        view.titik_banjir.setOnClickListener {
            jenis = "Titik Banjir"
            Toast.makeText(
                activity,
                "Anda melaporkan $jenis", Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
            tv_laporkan.text = "Laporkan $jenis"
        }
        return jenis
    }

    private fun uploadImage() {
        if (imageUri == null) {
            this.view?.snackbar("Select an Image First")
            return
        }

       // var base64 = "" //Your encoded string

       // base64 = "data:image/" + getMimeType(imageUri!!) + ";base64," + base64
//        val parcelFileDescriptor =
//            requireActivity().contentResolver?.openFileDescriptor(imageUri!!, "r", null) ?: return
//        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
//        val file =
//            File(context?.cacheDir, requireActivity().contentResolver.getFileName(imageUri!!))
//        val outputStream = FileOutputStream(file)
//        inputStream.copyTo(outputStream)

//        val body = UploadRequestBody(file, "image")

        val bearer = "Bearer " + Session.bearer

        val geometry = "{\"type\": \"Point\", \"coordinates\": ${editTextLokasi.text}}"
        val masyarakat = DataPengaduanMasyarakat(editTextNamaJalan.text.toString(),
            sImage,
            tv_laporkan.text.toString().substring(
                15),
            editTextDeskripsi.text.toString(),
            geometry)
        MyAPI().pengaduanMasyarakat(
            bearer,
            "application/json",
            masyarakat
//            editTextNamaJalan.text.toString()
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            //MultipartBody.Part.createFormData("foto", file.name, body),
//            base64.toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            tv_laporkan.text.toString().substring(15)
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            editTextDeskripsi.text.toString()
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            geometry.toRequestBody("multipart/form-data".toMediaTypeOrNull())

        ).enqueue(object : Callback<DataPengaduanMasyarakat> {
            override fun onResponse(
                call: Call<DataPengaduanMasyarakat>,
                response: Response<DataPengaduanMasyarakat>
            ) {
                val intent = Intent(activity, SuccessPage::class.java)
                intent.putExtra("type","login")
                startActivity(intent)
            }

            override fun onFailure(call: Call<DataPengaduanMasyarakat>, t: Throwable) {
                requireView().snackbar("gagal ${t.message}")
            }

        })

    }
    private fun uploadImageAnonym() {
        if (imageUri == null) {
            this.view?.snackbar("Select an Image First")
            return
        }
        val geometry = "{\"type\": \"Point\", \"coordinates\": ${editTextLokasi.text}}"
        val masyarakat = DataPengaduanMasyarakat(editTextNamaJalan.text.toString(),
            sImage,
            tv_laporkan.text.toString().substring(15),
            editTextDeskripsi.text.toString(),
            geometry)
        MyAPI().pengaduanMasyarakatAnonim(
            "application/json",
            masyarakat
//            editTextNamaJalan.text.toString()
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            //MultipartBody.Part.createFormData("foto", file.name, body),
//            base64.toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            tv_laporkan.text.toString().substring(15)
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            editTextDeskripsi.text.toString()
//                .toRequestBody("multipart/form-data".toMediaTypeOrNull()),
//            geometry.toRequestBody("multipart/form-data".toMediaTypeOrNull())

        ).enqueue(object : Callback<DataPengaduanMasyarakat> {
            override fun onResponse(
                call: Call<DataPengaduanMasyarakat>,
                response: Response<DataPengaduanMasyarakat>
            ) {
                val intent = Intent(activity, SuccessPage::class.java)
                intent.putExtra("type","anonim")
                startActivity(intent)
            }

            override fun onFailure(call: Call<DataPengaduanMasyarakat>, t: Throwable) {
                requireView().snackbar("gagal ${t.message}")
            }

        })
    }



}


