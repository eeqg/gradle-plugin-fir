package com.kycq.gradle.plugin.fir

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class FirApkInfoTask {
	String apiToken
	String bundleId
	static boolean IS_PRINT_INFO = true
	
	FirApkInfoTask(String apiToken, String bundleId) {
		this.apiToken = apiToken;
		this.bundleId = bundleId;
	}
	
	void printInfo() {
		if (!IS_PRINT_INFO) {
			return
		}
		
		JsonObject appInfoObject = getAppInfo(apiToken, bundleId)
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
	
	static String getAppId(String apiToken, String bundleId) {
		InputStream inputStream = null
		try {
			URL url = new URL("http://api.fir.im/apps?api_token=${apiToken}")
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
			
			JsonObject jsonObject = new Gson().fromJson(stringBuffer.toString(), JsonObject.class)
			JsonArray appArray = jsonObject.getAsJsonArray("items");
			for (int index = 0; index < appArray.size(); index++) {
				JsonObject itemObject = appArray.get(index).getAsJsonObject();
				String itemBundleId = itemObject.get("bundle_id").getAsString();
				if (bundleId == itemBundleId) {
					return itemObject.get("id").getAsString()
				}
			}
			return null
		} catch (Exception ignored) {
			ignored.printStackTrace()
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
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
			ignored.printStackTrace()
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
	}
}
