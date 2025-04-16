//package com.veggievision.lokatani
//import android.graphics.Bitmap
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//
//private fun saveBitmapToCache(bitmap: Bitmap): File {
//    // Mendapatkan direktori cache aplikasi
////    val cacheDir = cacheDir
//    // Membuat file sementara dengan nama tertentu
//    val file = File(cacheDir, "captured_image.png")
//
//    try {
//        // Menulis Bitmap ke file
//        FileOutputStream(file).use { outStream ->
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//    }
//
//    return file
//}
