package falkeadler.library.youtubedataextractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.interfaces.JsCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.net.URLDecoder
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
js functions example..... 아이고.....
 var fta = function(a) {
 	a = a.split("");
 	hD.mL(a, 79);
 	hD.L5(a, 2);
 	hD.mL(a, 24);
 	hD.L5(a, 3);
 	return a.join("")
 };
 var hD = {
 	i1: function(a, b) {
 		var c = a[0];
 		a[0] = a[b % a.length];
 		a[b % a.length] = c
 	},
 	L5: function(a, b) {
 		a.splice(0, b)
 	},
 	mL: function(a) {
 		a.reverse()
 	}
 };
 */

interface YouTubeDecipherService {
    @GET
    suspend fun getDecipherJsFile(@Url endpoint: String): ResponseBody
}

class DecipherMethodExtractor(val endpoint: String, val context: Context) {
    private val cacheDirectory: File by lazy {
        val cacheBase = context.cacheDir
        val jsCache = File(cacheBase, "JSCACHE")
        if (!jsCache.exists()) {
            jsCache.mkdir()
        }
        jsCache
    }
    private val parser = Json {
        isLenient = true
    }

    private val service: YouTubeDecipherService by lazy {
        val headerClient = OkHttpClient().newBuilder().addInterceptor(
            Interceptor {
                with(it) {
                    val newRequest = request().newBuilder().addHeader("User-Agent",
                        YouTubeDataExtractor.USER_AGENT
                    ).build()
                    proceed(newRequest)
                }
            }
        ).build()
        val logClient = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor(
            HttpLoggingInterceptor.Logger {
                Log.e("VidePlayerTestTAG", it)
            }).apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }).build()
        Retrofit.Builder().baseUrl("https://www.youtube.com/").client(headerClient).client(logClient).build().create(YouTubeDecipherService::class.java)
    }

    private val patternVariableFunction =
        Pattern.compile("([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\.([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(")

    private val patternSignatureDecipherFunction =
        Pattern.compile("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{1,4})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)")
    private val patternFunction = Pattern.compile("([{; =])([a-zA-Z$][a-zA-Z0-9$]{0,2})\\(")


    private fun loadCachedMethod(): CacheFunctionData? {
        if (cacheDirectory.exists()) {
            val filename = endpoint.replace("/", "_")
            val cacheFile = File(cacheDirectory, filename)
            if (cacheFile.exists() && cacheFile.isFile) {
                return FileInputStream(cacheFile).use {
                    Log.e("VideoPlayerTestTAG", "[CACHE] return cachedData!!!!")
                    parser.decodeFromStream<CacheFunctionData>(it)
                }

            }
        }
        return null
    }
    private fun saveCacheMethod(funcName: String, funcCode: String) {
        if (cacheDirectory.exists()) {
            val filename = endpoint.replace("/", "_")
            val cacheFile = File(cacheDirectory, filename)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            FileWriter(cacheFile).use {
                val data = CacheFunctionData(functionName = funcName, functionCode = funcCode)
                it.write(parser.encodeToString(data))
            }
            Log.e("VideoPlayerTestTAG", "[CACHE]saved = ${cacheFile.absolutePath}")
        }
    }
    private fun getDecipherMethod(jsFile: String): CacheFunctionData? {
        var matcher = patternSignatureDecipherFunction.matcher(jsFile)
        var decipherFunctionName: String?
        var decipherFunction: String?
        if (matcher.find()) {
            decipherFunctionName = matcher.group(1)
            val patternMainVariable = Pattern.compile(
                "(var |\\s|,|;)" + decipherFunctionName.replace("$", "\\$") +
                        "(=function\\((.{1,3})\\)\\{)"
            )
            var maintDecipherFunction: String?
            matcher = patternMainVariable.matcher(jsFile)
            if (matcher.find()) {
                maintDecipherFunction = "var " + decipherFunctionName + matcher.group(2)
            } else {
                val patternMainFunction = Pattern.compile(
                    "function " + decipherFunctionName.replace("$", "\\$") +
                            "(\\((.{1,3})\\)\\{)"
                )
                matcher = patternMainFunction.matcher(jsFile)
                if (!matcher.find()) {
                    return null
                }
                maintDecipherFunction = "function " + decipherFunctionName + matcher.group(2)
            }

            var startIndex = matcher.end()
            var braces = 1
            for (i in startIndex until jsFile.length) {
                if (braces == 0 && startIndex + 5 < i) {
                    maintDecipherFunction += jsFile.substring(startIndex, i)
                    maintDecipherFunction += ";"
                    break
                }
                if (jsFile[i] == '{') {
                    braces++
                } else if (jsFile[i] == '}') {
                    braces--
                }
            }
            decipherFunction = maintDecipherFunction
            matcher = patternVariableFunction.matcher(maintDecipherFunction)
            while(matcher.find()) {
                var variableDef = "var " + matcher.group(2) + "={"
                if (decipherFunction.contains(variableDef)) {
                    continue
                }
                startIndex = jsFile.indexOf(variableDef) + variableDef.length
                braces = 1
                for (i in startIndex until jsFile.length) {
                    if (braces == 0) {
                        decipherFunction += variableDef + jsFile.subSequence(startIndex, i)
                        decipherFunction += ";"
                        break
                    }
                    if (jsFile[i] == '{') {
                        braces++
                    } else if (jsFile[i] == '}') {
                        braces--
                    }
                }
            }
            matcher = patternFunction.matcher(maintDecipherFunction)
            while(matcher.find()) {
                var functionDef = "function " + matcher.group(2) + "("
                if(decipherFunction.contains(functionDef)) {
                    continue
                }
                startIndex = jsFile.indexOf(functionDef) + functionDef.length
                braces = 0
                for (i in startIndex until jsFile.length) {
                    if (braces == 0 && startIndex + 5 < i) {
                        decipherFunction += functionDef + jsFile.substring(startIndex, i)
                        decipherFunction += ";"
                        break
                    }
                    if (jsFile[i] == '{') {
                        braces++
                    } else if (jsFile[i] == '}') {
                        braces--
                    }
                }
            }
            saveCacheMethod(decipherFunctionName, decipherFunction)
            return CacheFunctionData(functionName = decipherFunctionName, functionCode = decipherFunction)
        } else {
           return null
        }
    }

    suspend fun decipher(signatureCipher: String): String? {
        val method = loadCachedMethod()?.let { it } ?: kotlin.run {
            val string = withContext(Dispatchers.IO) {
                service.getDecipherJsFile(endpoint).string()
            }
            getDecipherMethod(string)
        }
        return method?.let {
            val result = suspendCancellableCoroutine<String?> { continuation ->
                val split = signatureCipher.split("&")
                val url = split.filter {
                    str ->
                    str.startsWith("url=") }.map {
                    str ->
                    URLDecoder.decode(str.substring(4), "utf-8")
                }.first()
                val signature = split.filter { str-> str.startsWith("s=") }.map {
                    str ->
                    URLDecoder.decode(str.substring(2), "utf-8")
                }.first()
                val builder = java.lang.StringBuilder("${method.functionCode} function decipher(){return ")
                val js = builder.append(it.functionName).append("('").append(signature).append("')").append(System.lineSeparator()).append("};")
                    .append(System.lineSeparator()).append("decipher();").append(System.lineSeparator()).toString()

                Handler(Looper.getMainLooper()).post {
                    JsEvaluator(context).evaluate(js, object : JsCallback {
                        override fun onError(errorMessage: String?) {
                            Log.e("VideoPlayerTestTAG", "error ? = $errorMessage")
                            continuation.resumeWithException(RuntimeException("error"))
                        }

                        override fun onResult(value: String?) {

                            continuation.resume(value?.let {
                                "$url&sig=$it"
                            })
                        }
                    })
                }

            }
            Log.e("VideoPlayerTestTAG", "result = $result")
            result
        }
    }
}