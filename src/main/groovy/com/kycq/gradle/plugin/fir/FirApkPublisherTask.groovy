package com.kycq.gradle.plugin.fir

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FirApkPublisherTask extends DefaultTask {
	String LINE_END = "\r\n"
	String HYPHENS = "--"
	String BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
	
	String apiToken
	String bundleId
	
	File iconFile
	File apkFile
	String appName
	String downloadUrl
	
	String versionName
	String versionCode
	String changeLog
	
	String outputInfo
	
	@TaskAction
	void publish() {
		JsonObject jsonObject = getToken().getAsJsonObject("cert")
		
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
		
		println(outputInfo)
	}
	
	JsonObject getToken() {
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
			jsonObject.add("api_token", new JsonPrimitive(this.apiToken))
			jsonObject.add("bundle_id", new JsonPrimitive(this.bundleId))
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
	
	boolean uploadIcon(String iconFileUploadUrl,
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
	
	boolean uploadApk(String apkFileUploadUrl,
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
			dataOutputStream.writeBytes(appName)
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
			dataOutputStream.writeBytes(changeLog)
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

//	static void main(String[] argc) {
//		FirApkPublisherTask task = new FirApkPublisherTask()
//		task.iconFile = new File("/Users/kycq/StudioProjects/KDTLibraryDemo/upload.png")
//		task.apkFile = new File("/Users/kycq/StudioProjects/KDTLibraryDemo/app/build/outputs/apk/app-flavorTest-release.apk")
//		task.appName = "LibraryDemoTest"
//		task.versionName = "1.0.0"
//		task.versionCode = 1
//		task.changeLog = "version1.0.0"
//		task.apiToken = "ed23277146c1e20cf0e63671fa644bb6"
//		task.bundleId = "com.kdt.ste.safas.asfa"
//		task.publish()
//	}
}
