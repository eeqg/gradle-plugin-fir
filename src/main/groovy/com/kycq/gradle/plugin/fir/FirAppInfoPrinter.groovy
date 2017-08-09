package com.kycq.gradle.plugin.fir

import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.logging.Logger

class FirAppInfoPrinter {
	Logger logger
	ApplicationVariant variant
	String apiToken
	def productFlavorInfo
	
	void printInfo(boolean strictProductFlavor) {
		def productFlavorInfo = this.productFlavorInfo
		if (productFlavorInfo == null) {
			if (strictProductFlavor) {
				throw new NullPointerException("must add ${variant.name} productFlavorInfo at productFlavors")
			} else {
				return
			}
		}
		String bundleIdSuffix = productFlavorInfo.bundleIdSuffix
		if (bundleIdSuffix == null || bundleIdSuffix.length() == 0) {
			bundleIdSuffix = ""
		} else {
			bundleIdSuffix = ".${bundleIdSuffix}"
		}
		String bundleId = "${variant.applicationId}.${variant.name}${bundleIdSuffix}"
		
		JsonObject appInfoObject = getAppInfo(apiToken, bundleId)
		if (appInfoObject == null) {
			String apkInfo = new StringBuilder()
					.append("\n")
					.append("================== ${productFlavorInfo.appName} : ${bundleId} ==================")
					.append("\n")
					.append("can't find ${productFlavorInfo.appName} app, please confirm that you have uploaded successfully!")
			logger.error(apkInfo)
			println()
			return
		}
		String appName = appInfoObject.get("name").getAsString()
		String versionName = appInfoObject.get("versionShort").getAsString()
		String versionCode = appInfoObject.get("version").getAsString()
		String downloadUrl = appInfoObject.get("update_url").getAsString()
		
		String apkInfo = new StringBuilder()
				.append("\n")
				.append("================== ${appName} : ${bundleId} ==================")
				.append("\n")
				.append("versionName: ${versionName}  versionCode: ${versionCode} downloadUrl: ${downloadUrl}")
		println(apkInfo)
	}
	
	static JsonObject getAppInfo(String apiToken, String bundleId) {
		InputStream inputStream = null
		try {
			URL url = new URL("http://api.fir.im/apps/latest/${bundleId}?api_token=${apiToken}&type=android")
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection()
			
			httpURLConnection.setRequestMethod("GET")
			httpURLConnection.setConnectTimeout(10 * 1000)
			httpURLConnection.setReadTimeout(10 * 1000)
			httpURLConnection.setRequestProperty("Charset", "UTF-8")
			
			StringBuffer stringBuffer = new StringBuffer()
			inputStream = new BufferedInputStream(httpURLConnection.getInputStream())
			byte[] buffer = new byte[2048]
			int length
			while ((length = inputStream.read(buffer)) != -1) {
				stringBuffer.append(new String(buffer, 0, length))
			}
			
			return new Gson().fromJson(stringBuffer.toString(), JsonObject.class)
		} catch (Exception ignored) {
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
		return null
	}
}
