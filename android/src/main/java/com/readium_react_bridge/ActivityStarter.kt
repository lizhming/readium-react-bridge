package com.readium_react_bridge

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.mcxiaoke.koi.ext.close
import kotlinx.coroutines.launch
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.successUi
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.indeterminateProgressDialog
import org.readium.r2.shared.Publication
import org.readium.r2.streamer.parser.CbzParser
import org.readium.r2.streamer.parser.EpubParser
import org.readium.r2.streamer.server.Server
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.ServerSocket
import com.readium_react_bridge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.readium.r2.streamer.parser.PubBox
import org.zeroturnaround.zip.ZipUtil
import java.util.*
import org.jetbrains.anko.*
import org.readium.r2.navigator.R2CbzActivity
import org.readium.r2.shared.drm.DRM
import org.readium.r2.shared.parsePublication
import org.readium.r2.testapp.*
import org.readium.r2.testapp.audiobook.AudiobookActivity
import java.net.URI
import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.collections.forEachByIndex
import org.jetbrains.anko.design.*
import org.json.JSONObject
import org.readium.r2.testapp.drm.LCPLibraryActivityService
import org.readium.r2.testapp.permissions.PermissionHelper
import org.readium.r2.testapp.permissions.Permissions
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import javax.annotation.Nullable
import kotlin.coroutines.CoroutineContext

public class ActivityStarter (reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext), CoroutineScope, LCPLibraryActivityService {

    companion object {
        lateinit var reactContext: ReactApplicationContext

        fun staticChange(index: Int, total: Int, url: String) {
            val params: WritableMap = Arguments.createMap();
            params.putInt("index", index)
            params.putInt("total", total)
            params.putString("url", url.split("OEBPS/")[1].split(".xhtml")[0])

            sendEvent(reactContext,"pageChanged", params)
        }
        private fun sendEvent(reactContext: ReactContext, eventName: String, @Nullable params: WritableMap) {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(eventName, params)
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun getName(): String {
        return "RNReadiumReactBridge"
    }

    @ReactMethod
    fun navigateToReadium(filePath: String){
    ActivityStarter.reactContext = reactApplicationContext
//        val preferences = reactApplicationContext.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
//        localPort = preferences.getInt("publicationPort", 0)

        var s: ServerSocket? = null
        try {
            s = ServerSocket(0)
            s.localPort
            s.close()
            MainApplication.localPort = s.localPort
            MainApplication.server = Server(MainApplication.localPort)
            println("Server Created!")

            //SharedPreferences preferences = getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE);
            //preferences.edit().putString("publicationPort", ""+localPort).apply();
            if (!MainApplication.server.isAlive()) {
                try {
                    MainApplication.server.start()
                } catch (e: IOException) {
                    println("Server Error!")
                    // do nothing
                    Timber.e(e)
                }

                MainApplication.server.loadResources(reactApplicationContext.getAssets(), reactApplicationContext)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        println(filePath)
        val database = BooksDatabase(reactApplicationContext);
        books = database.books.list()

        println(books.count())
        //println(books[0].fileUrl)
        for (e in books){
            if (e.fileUrl.endsWith(filePath)){
                open(e)
                return
            }
        }

        println("Create new!")
        save(filePath)
        println("Close!")
        println(books.count())
        for (e in books){
            if (e.fileUrl.endsWith(filePath)){
                //open(e)
                return
            }
        }

//        if (bookId == 0) {
//            save(filePath)
//            return 0;
//        }
        //println(books);

        // -- AudiobookActivity,
        // publicationPath, <-path
        // epubName, <- book.fileName
        // publication <- publication
        // bookId <- book.id

        // -- R2CbzActivity,

        // -- R2EpubActivity
        //


//        val context = reactApplicationContext
//        val intent = Intent(context, CatalogActivity::class.java)
//        context.startActivity(intent)
        //open(0);

        return
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager: ConnectivityManager = reactApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    fun load(publicationPath: String) {
        val file = File(publicationPath)

        if (file.extension.equals("epub", ignoreCase = true)) {
            val parser = EpubParser()
            val pub = parser.parse(publicationPath)
            if (pub != null) {
                prepareToServe(pub, file.name, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
            }
        } else if (file.extension.equals("cbz", ignoreCase = true)) {
            val parser = CbzParser()
            val pub = parser.parse(publicationPath)
            if (pub != null) {
                prepareToServe(pub, file.name, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
            }
        }
    }

    private fun authorName(publication: Publication): String {
        return publication.metadata.authors.firstOrNull()?.name?.let {
            return@let it
        } ?: run {
            return@run String()
        }
    }
    protected fun prepareToServe(pub: PubBox?, fileName: String, absolutePath: String, add: Boolean, lcp: Boolean) {
        val database = BooksDatabase(reactApplicationContext);
        val preferences = reactApplicationContext.getSharedPreferences("org.readium.r2.settings", Context.MODE_PRIVATE)
        println("Prepare")
        if (pub == null) {
            return
        }
        val publication = pub.publication
        val container = pub.container
        println("Launch")
        launch {
            if (publication.type == Publication.TYPE.EPUB) {
                println("epub "+publication.metadata.identifier)
                val publicationIdentifier = publication.metadata.identifier
                preferences.edit().putString("$publicationIdentifier-publicationPort", MainApplication.localPort.toString()).apply()
                val author = authorName(publication)
                if (add) {
                    var book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                    publication.coverLink?.href?.let {
                        val blob = ZipUtil.unpackEntry(File(absolutePath), it.removePrefix("/"))
                        blob?.let {
                            book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, blob, Publication.EXTENSION.EPUB)
                        } ?: run {
                            book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                        }
                    } ?: run {
                        book = Book(fileName, publication.metadata.title, author, absolutePath, null, publication.coverLink?.href, publicationIdentifier, null, Publication.EXTENSION.EPUB)
                    }

                    if(!database.books.has(book.identifier).isEmpty()){
                        open(book)
                    }else{
                        database.books.insert(book, false)?.let {
                            book.id = it
                            books.add(0,book)
                            open(book)
                            println("inserted")
                            if (!lcp) {
                                //prepareSyntheticPageList(publication, book)
                            }
                        } ?: run {

                            //showDuplicateBookAlert(book, publication, lcp)

                        }
                    }

                }
                if (!lcp) {
                    println(publication)
                    println(container)
                    println("/$fileName")
                    println(reactApplicationContext.getExternalFilesDir(null)?.path + "/styles/UserProperties.json")
                    MainApplication.server.addEpub(publication, container, "/$fileName", reactApplicationContext.getExternalFilesDir(null)?.path + "/styles/UserProperties.json")
                }

            } else if (publication.type == Publication.TYPE.CBZ) {
                if (add) {
                    publication.coverLink?.href?.let {
                        val book = Book(fileName, publication.metadata.title, null, absolutePath, null, publication.coverLink?.href, UUID.randomUUID().toString(), container.data(it), Publication.EXTENSION.CBZ)
                        database.books.insert(book, false)?.let { id->
                            book.id = id
                            open(book)
                            if (!lcp) {
                                //prepareSyntheticPageList(publication, book)
                            }
                        } ?: run {

                            //showDuplicateBookAlert(book, publication, lcp)

                        }
                    }
                }
            }
        }
    }
//    var localPort: Int = 0

    fun save(path: String){
        val file = File(path)
        val progress = ProgressDialog(reactApplicationContext);
        println(file.extension)
        if (file.extension.equals("lcpl", ignoreCase = true)) {
            println("lcpl")
            processLcpActivityResult(Uri.fromFile(file), Uri.fromFile(file), progress, isNetworkAvailable)
        } else {
            println("epub")
            processEpubResult(Uri.fromFile(file), file.name, progress, file.name)//getMimeType(Uri.fromFile(file)).first
        }
    }

    private fun preparePublication(publicationPath: String, uriString: String, fileName: String, progress: ProgressDialog) {

        val file = File(publicationPath)

        try {
            launch {

                if (uriString.endsWith(".epub")) {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()
                    }
                } else if (uriString.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    if (pub != null) {
                        prepareToServe(pub, fileName, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        progress.dismiss()
                    }
                }

            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(uri: Uri): Pair<String, String> {
        val mimeType: String?
        var fileName = String()
        println(uri)
        println(uri.scheme)
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val contentResolver: ContentResolver = reactApplicationContext.contentResolver
            mimeType = contentResolver.getType(uri)
            getContentName(contentResolver, uri)?.let {
                fileName = it
            }
        } else {
            val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString())
            println(fileExtension)
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase())
        }
        return Pair(mimeType, fileName)
    }
    private fun getContentName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null)
        cursor!!.moveToFirst()
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        return if (nameIndex >= 0) {
            val name = cursor.getString(nameIndex)
            cursor.close()
            name
        } else {
            null
        }
    }
    private fun processEpubResult(uri: Uri?, mime: String?, progress: ProgressDialog, name: String) {
//        val fileName = UUID.randomUUID().toString()
//        val publicationPath = R2DIRECTORY + fileName
//
        val input = reactApplicationContext.contentResolver.openInputStream(uri)
        input?.toFile(uri?.path!! + "_ttt")
        val file = File(uri?.path)
        println(mime)

        try {
            launch {
                if (name.endsWith(".epub")){ //mime == "application/epub+zip") {
                    val parser = EpubParser()
                    println(file.absolutePath)
                    val pub = parser.parse(file.absolutePath)
                    if (pub != null) {
                        println("OOOOOOOOOO")
                        prepareToServe(pub, file.name, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        //progress.dismiss()

                    }
                } else if (name.endsWith(".cbz")) {
                    val parser = CbzParser()
                    val pub = parser.parse(file.absolutePath)
                    if (pub != null) {
                        prepareToServe(pub, file.name, file.absolutePath, add = true, lcp = pub.container.drm?.let { true } ?: false)
                        //progress.dismiss()

                    }
                } else {
                    //catalogView.longSnackbar("Unsupported file")
                    //progress.dismiss()
                    //file.delete()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun open(book: Book) {
        task {
            //val book = books[position]
            val publicationPath = book.fileUrl
            val file = File(publicationPath)
            when {
                book.ext == Publication.EXTENSION.EPUB -> {
                    val parser = EpubParser()
                    val pub = parser.parse(publicationPath)
                    pub?.let {
                        pub.container.drm?.let { drm: DRM ->
                            prepareAndStartActivityWithLCP(drm, pub, book, file, publicationPath, parser, pub.publication, isNetworkAvailable)
                        } ?: run {
                            prepareAndStartActivity(pub, book, file, publicationPath, pub.publication)
                        }
                    }
                }
                book.ext == Publication.EXTENSION.CBZ -> {
                    val parser = CbzParser()
                    val pub = parser.parse(publicationPath)
                    pub?.let {

                        val intent = Intent(reactApplicationContext, R2CbzActivity::class.java)
                        intent.putExtra("publicationPath", publicationPath);
                        intent.putExtra("cbzName", book.fileName);
                        intent.putExtra("publication", pub.publication);
                        intent.putExtra("bookId", book.id);

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        reactApplicationContext.startActivity(intent)
                    }
                }
                book.ext == Publication.EXTENSION.JSON -> {
                    prepareWebPublication(book.fileUrl, book, add = false)
                }
                else -> null
            }
        } then {

        } fail {

        }
    }
    private fun getBitmapFromURL(src: String): Bitmap? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            connection.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun prepareWebPublication(externalManifest: String, webPub: Book?, add: Boolean) {

        val database = BooksDatabase(reactApplicationContext);
        task {

            getPublicationURL(externalManifest)

        } then { json ->

            json?.let {
                val externalPub = parsePublication(json)
                val externalURI = externalPub.linkWithRel("self")!!.href!!.substring(0, externalManifest.lastIndexOf("/") + 1)

                var book: Book? = null

                if (add) {

                    externalPub.coverLink?.href?.let { href ->
                        val bitmap: Bitmap? = if (URI(href).isAbsolute) {
                            getBitmapFromURL(href)
                        } else {
                            getBitmapFromURL(externalURI + href)
                        }
                        val stream = ByteArrayOutputStream()
                        bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)

                        book = Book(externalURI, externalPub.metadata.title, null, externalManifest, null, externalURI + externalPub.coverLink?.href, externalPub.metadata.identifier, stream.toByteArray(), Publication.EXTENSION.JSON)

                    } ?: run {
                        book = Book(externalURI, externalPub.metadata.title, null, externalManifest, null, null, externalPub.metadata.identifier, null, Publication.EXTENSION.JSON)
                    }

                    launch {
                        database.books.insert(book!!, false)?.let { id ->
                            book!!.id = id
                            open(book!!)
                        } ?: run {

                        }
                    }
                } else {
                    book = webPub
                    startActivity(book!!.fileName, book!!, externalPub)
                }
            }
        }
    }

    private fun prepareAndStartActivity(pub: PubBox?, book: Book, file: File, publicationPath: String, publication: Publication) {
        prepareToServe(pub, book.fileName, file.absolutePath, add = false, lcp = false)
        startActivity(publicationPath, book, publication)
    }

    private fun getPublicationURL(src: String): JSONObject? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.doInput = true
            connection.connect()

            val jsonManifestURL = URL(connection.getHeaderField("Location") ?: src).openConnection()
            jsonManifestURL.connect()

            val jsonManifest = jsonManifestURL.getInputStream().readBytes()
            val stringManifest = jsonManifest.toString(Charset.defaultCharset())
            val json = JSONObject(stringManifest)

            jsonManifestURL.close()
            connection.disconnect()
            connection.close()

            json
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun startActivity(publicationPath: String, book: Book, publication: Publication) {
        if(publication.type == Publication.TYPE.AUDIO) {
            val intent = Intent(reactApplicationContext, AudiobookActivity::class.java)
            intent.putExtra("publicationPath", publicationPath);
            intent.putExtra("epubName", book.fileName);
            intent.putExtra("publication", publication);
            intent.putExtra("bookId", book.id);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            reactApplicationContext.startActivity(intent)

        } else {
            val intent = Intent(reactApplicationContext, R2EpubActivity::class.java)
            intent.putExtra("publicationPath", publicationPath);
            intent.putExtra("epubName", book.fileName);
            intent.putExtra("publication", publication);
            intent.putExtra("bookId", book.id);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            reactApplicationContext.startActivity(intent)
        }
    }

    override fun parseIntentLcpl(uriString: String, networkAvailable: Boolean) {
        listener?.parseIntentLcpl(uriString, networkAvailable)
    }

    override fun prepareAndStartActivityWithLCP(drm: DRM, pub: PubBox, book: Book, file: File, publicationPath: String, parser: EpubParser, publication: Publication, networkAvailable: Boolean) {
        listener?.prepareAndStartActivityWithLCP(drm, pub, book, file, publicationPath, parser, publication, networkAvailable)
    }

    override fun processLcpActivityResult(uri: Uri, it: Uri, progress: ProgressDialog, networkAvailable: Boolean) {
        listener?.processLcpActivityResult(uri,it,progress, networkAvailable)
    }
    protected var listener:ActivityStarter? = null


}