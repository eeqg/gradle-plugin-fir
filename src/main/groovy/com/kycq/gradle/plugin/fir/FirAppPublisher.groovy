package com.kycq.gradle.plugin.fir

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

import java.nio.charset.Charset

class FirAppPublisher {
	static String LINE_END = "\r\n"
	static String HYPHENS = "--"
	static String BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
	
	File iconFile
	ApplicationVariant variant
	String apiToken
	def publicVersionInfo
	def productFlavorInfo
	
	void publish(boolean strictProductFlavor) {
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
		
		def apkOutput = variant.outputs.find { variantOutput -> variantOutput instanceof ApkVariantOutput }
		File apkFile = apkOutput.outputFile
		String appName = productFlavorInfo.appName
		String versionName = variant.versionName
		String versionCode = variant.versionCode
		String changeLog
		if (productFlavorInfo.versionInfo != null) {
			changeLog = productFlavorInfo.versionInfo[variant.versionName]
		}
		if (changeLog == null) {
			if (publicVersionInfo == null) {
				throw new NullPointerException("must config versionInfo at json.root or jsonRoot.productFlavors.${variant.name}")
			}
			changeLog = publicVersionInfo[variant.versionName]
		}
		if (changeLog == null) {
			throw new NullPointerException("must config ${variant.versionName} version log at versionInfo or jsonRoot.productFlavors.${variant.name}.versionInfo")
		}
		
		JsonObject jsonObject = getToken(apiToken, bundleId).getAsJsonObject("cert")
		
		// upload icon
		if (iconFile != null) {
			JsonObject iconObject = jsonObject.getAsJsonObject("icon")
			String iconFileUploadUrl = iconObject.get("upload_url").getAsString()
			String iconFileKey = iconObject.get("key").getAsString()
			String iconFileToken = iconObject.get("token").getAsString()
			boolean uploadIconResult = uploadIcon(iconFileUploadUrl, iconFileKey, iconFileToken, iconFile)
			if (!uploadIconResult) {
				throw new RuntimeException("upload icon failure")
			}
		}
		
		// upload file
		JsonObject apkObject = jsonObject.getAsJsonObject("binary")
		String apkFileUploadUrl = apkObject.get("upload_url").getAsString()
		String apkFileKey = apkObject.get("key").getAsString()
		String apkFileToken = apkObject.get("token").getAsString()
		
		boolean uploadApkResult = uploadApk(apkFileUploadUrl, apkFileKey, apkFileToken, apkFile, appName, versionName, versionCode, changeLog)
		if (!uploadApkResult) {
			throw new RuntimeException("upload apk failure")
		}
	}
	
	static JsonObject getToken(String apiToken, String bundleId) {
		DataOutputStream outputStream = null
		InputStream inputStream = null
		StringBuffer stringBuffer = null
		try {
			URL url = new URL("http://api.fir.im/apps")
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection()
			
			httpURLConnection.setRequestMethod("POST")
			httpURLConnection.setDoOutput(true)
			httpURLConnection.setUseCaches(false)
			httpURLConnection.setConnectTimeout(10 * 1000)
			httpURLConnection.setReadTimeout(10 * 1000)
			httpURLConnection.setRequestProperty("Content-Type", "application/json")
			httpURLConnection.setRequestProperty("Charset", "UTF-8")
			
			outputStream = new DataOutputStream(httpURLConnection.getOutputStream())
			JsonObject jsonObject = new JsonObject()
			jsonObject.add("type", new JsonPrimitive("android"))
			jsonObject.add("api_token", new JsonPrimitive(apiToken))
			jsonObject.add("bundle_id", new JsonPrimitive(bundleId))
			outputStream.writeBytes(jsonObject.toString())
			outputStream.flush()
			outputStream.close()
			
			stringBuffer = new StringBuffer()
			inputStream = new BufferedInputStream(httpURLConnection.getInputStream())
			byte[] buffer = new byte[2048]
			int length
			while ((length = inputStream.read(buffer)) != -1) {
				stringBuffer.append(new String(buffer, 0, length))
			}
			return new Gson().fromJson(stringBuffer.toString(), JsonObject.class)
		} catch (Exception ignored) {
			if (stringBuffer != null) {
				throw new RuntimeException("getToken failure : " + stringBuffer.toString())
			} else {
				throw ignored
			}
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close()
				} catch (IOException ignored) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
	}
	
	static boolean uploadIcon(String iconFileUploadUrl,
	                          String iconFileKey, String iconFileToken,
	                          File iconFile) {
		FileInputStream fileInputStream = null
		InputStream inputStream = null
		byte[] buffer = new byte[2048]
		int length
		
		try {
			URL url = new URL(iconFileUploadUrl)
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection()
			
			httpURLConnection.setRequestMethod("POST")
			httpURLConnection.setDoInput(true)
			httpURLConnection.setDoOutput(true)
			httpURLConnection.setUseCaches(false)
			httpURLConnection.setChunkedStreamingMode(4096)
			httpURLConnection.setConnectTimeout(10 * 1000)
			httpURLConnection.setReadTimeout(10 * 1000)
			
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
			httpURLConnection.setRequestProperty("Accept", "*/*")
			httpURLConnection.setRequestProperty("Cache-Control", "no-cache")
			httpURLConnection.setRequestProperty("Charset", "UTF-8")
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
			
			DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream())
			
			// iconFileKey
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"key\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(iconFileKey)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// iconFileToken
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"token\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(iconFileToken)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// iconFile
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${iconFile.getName()}\"" + LINE_END)
			dataOutputStream.writeBytes("Content-Type: application/octet-stream" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			fileInputStream = new FileInputStream(iconFile)
			while ((length = fileInputStream.read(buffer)) != -1) {
				dataOutputStream.write(buffer, 0, length)
				dataOutputStream.flush()
			}
			fileInputStream.close()
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			dataOutputStream.close()
			
			StringBuilder stringBuilder = new StringBuilder()
			inputStream = new BufferedInputStream(httpURLConnection.getInputStream())
			while ((length = inputStream.read(buffer)) != -1) {
				stringBuilder.append(new String(buffer, 0, length))
			}
			JsonObject jsonObject = new Gson().fromJson(stringBuilder.toString(), JsonObject.class)
			return jsonObject != null && jsonObject.has("is_completed")
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close()
				} catch (IOException ignored) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
	}
	
	static boolean uploadApk(String apkFileUploadUrl,
	                         String apkFileKey, String apkFileToken,
	                         File apkFile, String appName,
	                         String versionName, String versionCode, String changeLog) {
		FileInputStream fileInputStream = null
		InputStream inputStream = null
		byte[] buffer = new byte[2048]
		int length
		
		try {
			URL url = new URL(apkFileUploadUrl)
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection()
			
			httpURLConnection.setRequestMethod("POST")
			httpURLConnection.setDoInput(true)
			httpURLConnection.setDoOutput(true)
			httpURLConnection.setUseCaches(false)
			httpURLConnection.setChunkedStreamingMode(4096)
			httpURLConnection.setConnectTimeout(10 * 1000)
			httpURLConnection.setReadTimeout(10 * 1000)
			
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
			httpURLConnection.setRequestProperty("Accept", "*/*")
			httpURLConnection.setRequestProperty("Cache-Control", "no-cache")
			httpURLConnection.setRequestProperty("Charset", "UTF-8")
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
			
			DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream())
			
			// apkFileKey
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"key\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(apkFileKey)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// apkFileToken
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"token\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(apkFileToken)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// apkFile
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${apkFile.getName()}\"" + LINE_END)
			dataOutputStream.writeBytes("Content-Type: application/octet-stream" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			fileInputStream = new FileInputStream(apkFile)
			while ((length = fileInputStream.read(buffer)) != -1) {
				dataOutputStream.write(buffer, 0, length)
				dataOutputStream.flush()
			}
			fileInputStream.close()
			dataOutputStream.writeBytes(LINE_END)
			// appName
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:name\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.write(appName.getBytes(Charset.forName("UTF-8")))
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// versionName
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:version\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(versionName)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// versionCode
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:build\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.writeBytes(versionCode)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			// changeLog
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + LINE_END)
			dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"x:changelog\"" + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.write(changeLog.getBytes(Charset.forName("UTF-8")))
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			
			dataOutputStream.writeBytes(HYPHENS + BOUNDARY + HYPHENS + LINE_END)
			dataOutputStream.writeBytes(LINE_END)
			dataOutputStream.flush()
			dataOutputStream.close()
			
			StringBuilder stringBuilder = new StringBuilder()
			inputStream = new BufferedInputStream(httpURLConnection.getInputStream())
			while ((length = inputStream.read(buffer)) != -1) {
				stringBuilder.append(new String(buffer, 0, length))
			}
			JsonObject jsonObject = new Gson().fromJson(stringBuilder.toString(), JsonObject.class)
			return jsonObject != null && jsonObject.has("is_completed")
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close()
				} catch (IOException ignored) {
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close()
				} catch (IOException ignored) {
				}
			}
		}
	}
}
