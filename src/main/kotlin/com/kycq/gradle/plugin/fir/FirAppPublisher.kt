package com.kycq.gradle.plugin.fir

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class FirAppPublisher {
	private val LINE_END = "\r\n"
	private val HYPHENS = "--"
	private val BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
	
	var iconFile: File? = null
	lateinit var variant: ApplicationVariant
	lateinit var apiToken: String
	var publicVersionInfo: JsonObject? = null
	var productFlavorInfo: JsonObject? = null
	
	fun publish(strictProductFlavor: Boolean) {
		val productFlavorInfo = this.productFlavorInfo ?: if (strictProductFlavor) {
			throw NullPointerException("must add ${variant.name} productFlavorInfo at productFlavors")
		} else {
			return
		}
		var bundleIdSuffix = productFlavorInfo.getAsJsonPrimitive("bundleIdSuffix")?.asString
		bundleIdSuffix = if (bundleIdSuffix == null || bundleIdSuffix.isEmpty()) {
			""
		} else {
			".$bundleIdSuffix"
		}
		
		val bundleId = "${variant.applicationId}.${variant.name}$bundleIdSuffix"
		val apkOutput = variant.outputs.find { variantOutput -> variantOutput is ApkVariantOutput }
		val apkFile = apkOutput!!.outputFile
		val appName = productFlavorInfo.getAsJsonPrimitive("appName").asString
		val versionName = variant.versionName
		val versionCode: String = variant.versionCode.toString()
		var changeLog: String? = null
		if (productFlavorInfo.get("versionInfo") != null) {
			changeLog = productFlavorInfo.getAsJsonObject("versionInfo").getAsJsonPrimitive(variant.versionName)?.asString
		}
		if (changeLog == null) {
			if (publicVersionInfo == null) {
				throw NullPointerException("must config versionInfo at json.root or jsonRoot.productFlavors.${variant.name}")
			}
			changeLog = publicVersionInfo!!.getAsJsonPrimitive(variant.versionName).asString
		}
		if (changeLog == null) {
			throw NullPointerException("must config ${variant.versionName} version log at versionInfo or jsonRoot.productFlavors.${variant.name}.versionInfo")
		}
		val jsonObject = getToken(apiToken, bundleId)!!.getAsJsonObject("cert")
		
		// upload icon
		if (iconFile != null) {
			val iconObject = jsonObject.getAsJsonObject("icon")
			val iconFileUploadUrl = iconObject.get("upload_url").asString
			val iconFileKey = iconObject.get("key").asString
			val iconFileToken = iconObject.get("token").asString
			val uploadIconResult = uploadIcon(iconFileUploadUrl, iconFileKey, iconFileToken, iconFile!!)
			if (!uploadIconResult) {
				throw RuntimeException("upload icon failure")
			}
		}
		
		// upload file
		val apkObject = jsonObject.getAsJsonObject("binary")
		val apkFileUploadUrl = apkObject.get("upload_url").asString
		val apkFileKey = apkObject.get("key").asString
		val apkFileToken = apkObject.get("token").asString
		
		val uploadApkResult = uploadApk(apkFileUploadUrl, apkFileKey, apkFileToken, apkFile, appName, versionName, versionCode, changeLog)
		if (!uploadApkResult) {
			throw RuntimeException("upload apk failure")
		}
	}
	
	private fun getToken(apiToken: String, bundleId: String): JsonObject? {
		val url = URL("http://api.fir.im/apps")
		val httpURLConnection = url.openConnection() as HttpURLConnection
		
		httpURLConnection.requestMethod = "POST"
		httpURLConnection.doOutput = true
		httpURLConnection.useCaches = false
		httpURLConnection.connectTimeout = 10 * 1000
		httpURLConnection.readTimeout = 10 * 1000
		
		httpURLConnection.setRequestProperty("Content-Type", "application/json")
		httpURLConnection.setRequestProperty("Charset", "UTF-8")
		
		val outputStream = DataOutputStream(httpURLConnection.outputStream)
		val jsonObject = JsonObject()
		jsonObject.add("type", JsonPrimitive("android"))
		jsonObject.add("api_token", JsonPrimitive(apiToken))
		jsonObject.add("bundle_id", JsonPrimitive(bundleId))
		outputStream.writeBytes(jsonObject.toString())
		outputStream.flush()
		outputStream.close()
		
		val bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
		val resultStr = bufferedReader.readText()
		
		return Gson().fromJson(resultStr, JsonObject::class.java)
	}
	
	private fun uploadIcon(iconFileUploadUrl: String,
	                       iconFileKey: String, iconFileToken: String,
	                       iconFile: File): Boolean {
		val url = URL(iconFileUploadUrl)
		val httpURLConnection = url.openConnection() as HttpURLConnection
		
		httpURLConnection.requestMethod = "POST"
		httpURLConnection.doInput = true
		httpURLConnection.doOutput = true
		httpURLConnection.useCaches = false
		httpURLConnection.connectTimeout = 10 * 1000
		httpURLConnection.readTimeout = 10 * 1000
		httpURLConnection.setChunkedStreamingMode(4096)
		
		httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
		httpURLConnection.setRequestProperty("Accept", "*/*")
		httpURLConnection.setRequestProperty("Cache-Control", "no-cache")
		httpURLConnection.setRequestProperty("Charset", "UTF-8")
		httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
		
		val dataOutputStream = DataOutputStream(httpURLConnection.outputStream)
		
		// iconFileKey
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"key\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(iconFileKey)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// iconFileToken
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"token\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(iconFileToken)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// iconFile
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${iconFile.name}\"$LINE_END")
		dataOutputStream.writeBytes("Content-Type: application/octet-stream" + LINE_END)
		dataOutputStream.writeBytes(LINE_END)
		val fileInputStream = FileInputStream(iconFile)
		fileInputStream.copyTo(dataOutputStream)
		fileInputStream.close()
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		dataOutputStream.close()
		
		val bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
		val resultStr = bufferedReader.readText()
		
		val jsonObject = Gson().fromJson(resultStr, JsonObject::class.java)
		return jsonObject != null && jsonObject.has("is_completed")
	}
	
	private fun uploadApk(apkFileUploadUrl: String,
	                      apkFileKey: String, apkFileToken: String,
	                      apkFile: File, appName: String,
	                      versionName: String, versionCode: String, changeLog: String): Boolean {
		val url = URL(apkFileUploadUrl)
		val httpURLConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
		
		httpURLConnection.requestMethod = "POST"
		httpURLConnection.doInput = true
		httpURLConnection.doOutput = true
		httpURLConnection.useCaches = false
		httpURLConnection.setChunkedStreamingMode(4096)
		httpURLConnection.connectTimeout = 10 * 1000
		httpURLConnection.readTimeout = 10 * 1000
		
		httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
		httpURLConnection.setRequestProperty("Accept", "*/*")
		httpURLConnection.setRequestProperty("Cache-Control", "no-cache")
		httpURLConnection.setRequestProperty("Charset", "UTF-8")
		httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
		
		val dataOutputStream = DataOutputStream(httpURLConnection.outputStream)
		
		// apkFileKey
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"key\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(apkFileKey)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// apkFileToken
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"token\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(apkFileToken)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// apkFile
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${apkFile.name}\"$LINE_END")
		dataOutputStream.writeBytes("Content-Type: application/octet-stream$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		val fileInputStream = FileInputStream(apkFile)
		fileInputStream.copyTo(dataOutputStream)
		fileInputStream.close()
		dataOutputStream.writeBytes(LINE_END)
		// appName
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:name\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.write(appName.toByteArray(Charset.forName("UTF-8")))
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// versionName
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:version\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(versionName)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// versionCode
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:build\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.writeBytes(versionCode)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		// changeLog
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:changelog\"$LINE_END")
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.write(changeLog.toByteArray(Charset.forName("UTF-8")))
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		
		dataOutputStream.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END)
		dataOutputStream.writeBytes(LINE_END)
		dataOutputStream.flush()
		dataOutputStream.close()
		
		val bufferedReader = BufferedReader(InputStreamReader(httpURLConnection.inputStream,"UTF-8"))
		val resultStr = bufferedReader.readText()
		
		val jsonObject = Gson().fromJson(resultStr, JsonObject::class.java)
		return jsonObject != null && jsonObject.has("is_completed")
	}
}