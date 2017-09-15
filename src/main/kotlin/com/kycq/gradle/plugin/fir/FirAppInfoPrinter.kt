package com.kycq.gradle.plugin.fir

import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

open class FirAppInfoPrinter {
	lateinit var logger: Logger
	lateinit var variant: ApplicationVariant
	lateinit var apiToken: String
	var productFlavorInfo: JsonObject? = null
	
	fun printInfo(strictProductFlavor: Boolean) {
		val productFlavorInfo = this.productFlavorInfo ?: if (strictProductFlavor) {
			throw NullPointerException("must add ${variant.name} productFlavorInfo at productFlavors")
		} else {
			return
		}
		val apiToken = this.apiToken
		var bundleIdSuffix = productFlavorInfo.getAsJsonPrimitive("bundleIdSuffix")?.asString
		bundleIdSuffix = if (bundleIdSuffix == null || bundleIdSuffix.isEmpty()) {
			""
		} else {
			".$bundleIdSuffix"
		}
		val bundleId = "${variant.applicationId}.${variant.name}$bundleIdSuffix"
		
		val appInfoObject = getAppInfo(apiToken, bundleId)
		if (appInfoObject == null) {
			val apkInfo = StringBuilder()
					.append("\n")
					.append("================== ${productFlavorInfo.getAsJsonPrimitive("appName").asString} : $bundleId ==================")
					.append("\n")
					.append("can't find ${productFlavorInfo.getAsJsonPrimitive("appName").asString} app, please confirm that you have uploaded successfully!")
			logger.error(apkInfo.toString())
			println()
			return
		}
		
		val appName = appInfoObject.get("name").asString
		val versionName = appInfoObject.get("versionShort").asString
		val versionCode = appInfoObject.get("version").asString
		val downloadUrl = appInfoObject.get("update_url").asString
		
		val apkInfo = StringBuilder()
				.append("\n")
				.append("================== $appName : $bundleId ==================")
				.append("\n")
				.append("versionName: $versionName  versionCode: $versionCode downloadUrl: $downloadUrl")
		println(apkInfo)
	}
	
	private fun getAppInfo(apiToken: String, bundleId: String): JsonObject? {
		val url = URL("http://api.fir.im/apps/latest/$bundleId?api_token=$apiToken&type=android")
		val httpURLConnection = url.openConnection() as HttpURLConnection
		
		httpURLConnection.requestMethod = "GET"
		httpURLConnection.connectTimeout = 10 * 1000
		httpURLConnection.readTimeout = 10 * 1000
		httpURLConnection.setRequestProperty("Charset", "UTF-8")
		
		val builder = StringBuilder()
		val inputStream = httpURLConnection.inputStream
		val buffer = ByteArray(2048)
		var length : Int
		while (true) {
			length = inputStream.read(buffer)
			if (length <= 0) {
				break
			}
			builder.append(String(buffer, 0, length))
		}
//		val bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream, "UTF-8"))
//		val resultStr = bufferedReader.readText()
		println("=============")
		println(builder.toString())
//		bufferedReader.close()
		
		return Gson().fromJson(builder.toString(), JsonObject::class.java)
	}
}