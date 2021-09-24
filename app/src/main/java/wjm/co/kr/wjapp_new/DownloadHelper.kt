package wjm.co.kr.wjapp_new

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import android.os.StrictMode
import android.util.Base64
import android.util.Base64.encodeToString
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.*
import java.net.URLDecoder

object DownloadHelper {

    private var downloadReference: Long = 0
    private lateinit var downloadManager: DownloadManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != downloadReference) {
                    context.unregisterReceiver(this)
                    return
                }
                val query = DownloadManager.Query()
                query.setFilterById(downloadReference)
                val cursor = downloadManager.query(query)
                cursor?.let {
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            val localFile = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            var locFile = ""


                            if (localFile.contains("file:///")) {
                                locFile = URLDecoder.decode(localFile.removePrefix("file:///"), "UTF-8")
//                                localFile = localFile.removePrefix("file:///").substringBeforeLast(File.separator)
                            }
//                            println(locFile)
//                            println(localFile)

                            val file = File(locFile)
                            val mime = MimeTypeMap.getSingleton()
                            val index = file.name.lastIndexOf('.') + 1
                            val ext = file.name.substring(index).toLowerCase()
                            val type = mime.getMimeTypeFromExtension(ext)

//                            println("mime ytpe: " + type)
                            val builder = StrictMode.VmPolicy.Builder()
                            StrictMode.setVmPolicy(builder.build())
                            val uri = Uri.fromFile(file)
                            val fileintent = Intent(Intent.ACTION_VIEW)
                            fileintent.setDataAndType(uri, type)
                            fileintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            try {
                                context.startActivity(fileintent)
                            } catch (e:ActivityNotFoundException) {
                                Toast.makeText(context, "해당파일을 실행할 수 있는 어플리케이션이 없습니다.", Toast.LENGTH_LONG).show()
                            }

                            Toast.makeText(context, "저장완료", Toast.LENGTH_LONG).show()

                        } else if (DownloadManager.STATUS_FAILED == cursor.getInt(columnIndex)) {
                            Toast.makeText(context, "다운로드 실패" + cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)), Toast.LENGTH_LONG).show()
                            Log.d("handleData()", "Reason: " + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)))
                        }
                    }
                    cursor.close()
                }

                context.unregisterReceiver(this)

            }
        }
    }

    fun downloadFile(url: String, mimeType: String? = null, context:Context, nmfile: String, publicId: String, publicPw: String) {

        downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri)
        val basicAuth = "Basic " + encodeToString(String.format("%s:%s",publicId,publicPw).toByteArray(), Base64.DEFAULT)
        request.addRequestHeader("Authorization" , basicAuth)

        request.apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            //setAllowedOverRoaming(true)
            setTitle(nmfile)
            setDescription(nmfile)
            setVisibleInDownloadsUi(true)
            allowScanningByMediaScanner()
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            //request.setDestinationUri(Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)))
            setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS, nmfile)
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

            downloadReference = downloadManager.enqueue(this)
        }
    }

//    private val bgContext: CoroutineContext = CommonPool
//
//    fun downloadAndShare(url: String?, mimeType: String? = null) = async(bgContext) {
//
//        var input: InputStream? = null
//        var output: OutputStream? = null
//        var connection: HttpURLConnection? = null
//
//        val tempVideoPath = """${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path}${File.separator}${URLUtil.guessFileName(url, null, mimeType)}"""
//        val targetFile = File(tempVideoPath)
//
//        try {
//            val urlConnection = URL(url)
//            connection = urlConnection.openConnection() as HttpURLConnection
//            connection.connect()
//
//            // expect HTTP 200 OK, so we don't mistakenly save error report
//            // instead of the file
//            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
//         //       Timber.e("""Server returned HTTP ${connection.responseCode} ${connection.responseMessage}""")
//                return@async null
//            }
//
//            // download the file
//            //isDownloadingVideo = true
//
//            input = connection.inputStream
//
//            input?.let {
//                output = FileOutputStream(targetFile, false)
//
//                /*val videoSize: Long
//
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                    videoSize = connection.contentLengthLong
//                } else {
//                    videoSize = connection.contentLength.toLong()
//                }
//
//                var currentSize: Long = 0*/
//
//                val data = ByteArray(4096)
//                var count: Int
//
//                do {
//                    count = input.read(data)
//                    if (count != 1) {
//                        output!!.write(data, 0, count)
//
//                        /*currentSize += count.toLong()
//                        if (publishSubject != null) {
//                            if (videoSize != 0L) {
//                                publishSubject.onNext((100 * currentSize / videoSize).toString())
//                            }
//                        }*/
//
//                    } else {
//                        break
//                    }
//
//                } while (count != -1)
//            }
//
//        } catch (e: Exception) {
//        //    Timber.e(e.message)
//        } finally {
//            try {
//                output?.close()
//                input?.close()
//                connection?.disconnect()
//            } catch (e: IOException) {
//          //      Timber.e(e.message)
//            }
//        }
//
//        if (targetFile.exists()) targetFile.path else null
//    }
}