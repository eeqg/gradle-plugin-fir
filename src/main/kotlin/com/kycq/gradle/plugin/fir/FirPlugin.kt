package com.kycq.gradle.plugin.fir

import com.android.build.gradle.AppExtension
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.FileReader

open class FirPlugin : Plugin<Project> {
	companion object {
		const val FIR_APP_EXTENSION = "firPublisher"
		const val API_TOKEN = "apiToken"
		const val VERSION_INFO = "versionInfo"
		const val PRODUCT_FLAVORS = "productFlavors"
		const val ASSEMBLE = "assemble"
		const val GROUP_NAME = "publish"
	}
	
	override fun apply(project: Project) {
		project.extensions.create(FIR_APP_EXTENSION, FirAppExtension::class.java)
		
		project.afterEvaluate {
			var gitUrl = ""
			var gitBranch = ""
			var gitBranchPrefix = ""
			val gitDir = File(project.rootDir.path + "/.git")
			if (gitDir.exists()) {
				val gitConfigFile = File(project.rootDir.path + "/.git/config")
				val fileReader = FileReader(gitConfigFile)
				fileReader.readLines().forEach {
					val lineText = it.trim()
					if (lineText.contains("url = ")) {
						gitUrl = lineText.split(" = ")[1]
					}
				}
				
				val gitHeadFile = File(project.rootDir.path + "/.git/HEAD")
				gitBranch = gitHeadFile.readText()
						.replace("ref: refs/heads/", "")
						.replace("\n", "")
				val gitBranchArray = gitBranch.split("/")
				val gitBranchBuilder = StringBuilder()
				gitBranchArray.forEach {
					gitBranchBuilder.append(it[0].toUpperCase())
							.append(it.subSequence(1, it.length))
				}
				gitBranchPrefix = gitBranchBuilder.toString()
			}
			
			if (gitBranch.isEmpty()) {
				gitBranch = "master"
				gitBranchPrefix = "master"
			}
			
			val projectName = project.name.substring(0, 1).toUpperCase() + project.name.substring(1)
			
			val firAppExtension = project.extensions.findByType(FirAppExtension::class.java)
			val infoFile = firAppExtension.infoFile
			when (infoFile) {
				null -> throw  NullPointerException("must config firPublisher.infoFile it build.gradle.")
			}
			val jsonInfoObject = Gson().fromJson(infoFile!!.readText(), JsonObject::class.java)
			val apiToken = jsonInfoObject.getAsJsonPrimitive(API_TOKEN).asString
			when (apiToken) {
				null -> throw NullPointerException("must config apiToken at jsonRoot in $infoFile file.")
			}
			val versionInfo = jsonInfoObject.getAsJsonObject(VERSION_INFO)
			val productFlavorsObject = jsonInfoObject.getAsJsonObject(PRODUCT_FLAVORS)
			when (productFlavorsObject) {
				null -> NullPointerException("must config productFlavors at JsonRoot in $infoFile file.")
			}
			
			val iconFile = firAppExtension.iconFile
			
			val firAppPublisherTask = project.tasks.create("publishFir", FirAppPublisherTask::class.java)
			firAppPublisherTask.dependsOn(project.getTasksByName(ASSEMBLE, false))
			firAppPublisherTask.group = GROUP_NAME
			firAppPublisherTask.singlePrinter = false
			
			val firAppInfoPrinterTask = project.tasks.create("printFir", FirAppPrinterTask::class.java)
			firAppInfoPrinterTask.group = GROUP_NAME
			firAppInfoPrinterTask.singlePrinter = false
			
			val androidExtension = project.extensions.findByName("android") as AppExtension
			androidExtension.applicationVariants.forEach { variant ->
				val name = variant.name.substring(0, 1).toUpperCase() + variant.name.substring(1)
				
				if (!(firAppExtension.jenkinsUrl == null
						&& firAppExtension.jenkinsAuthorization == null
						&& firAppExtension.jenkinsCredentialsId == null
						&& firAppExtension.jenkinsTaskGradleName == null)) {
					if (firAppExtension.jenkinsUrl == null) {
						throw RuntimeException("must config jenkinsUrl")
					}
					if (firAppExtension.jenkinsAuthorization == null) {
						throw RuntimeException("must config jenkinsAuthorization")
					}
					if (firAppExtension.jenkinsCredentialsId == null) {
						throw RuntimeException("must config jenkinsCredentialsId")
					}
					if (firAppExtension.jenkinsTaskGradleName == null) {
						throw RuntimeException("must config jenkinsTaskGradleName")
					}
					
					val jenkinsTask = project.tasks.create("jenkinsJob$name", JenkinsJobTask::class.java)
					jenkinsTask.group = GROUP_NAME
					jenkinsTask.gitToolPath = firAppExtension.gitToolPath
					jenkinsTask.gitUrl = gitUrl
					jenkinsTask.gitBranch = gitBranch
					
					jenkinsTask.jobName = project.rootProject.name + projectName + gitBranchPrefix + name
					jenkinsTask.jenkinsUrl = firAppExtension.jenkinsUrl!!
					jenkinsTask.jenkinsAuthorization = firAppExtension.jenkinsAuthorization!!
					jenkinsTask.jenkinsCredentialsId = firAppExtension.jenkinsCredentialsId!!
					jenkinsTask.jenkinsTaskName = "publishFir$name"
					jenkinsTask.jenkinsTaskGradleName = firAppExtension.jenkinsTaskGradleName!!
				}
				
				val firVariantPublisherTask = project.tasks.create("publishFir$name", FirAppPublisherTask::class.java)
				firVariantPublisherTask.dependsOn(variant.assemble)
				firVariantPublisherTask.group = GROUP_NAME
				
				val firVariantInfoPrinterTask = project.tasks.create("printFir$name", FirAppPrinterTask::class.java)
				firVariantInfoPrinterTask.group = GROUP_NAME
				
				val productFlavorObject = jsonInfoObject.getAsJsonObject(PRODUCT_FLAVORS)
				var productFlavorInfo = productFlavorObject.getAsJsonObject(variant.name)
				if (productFlavorInfo == null && variant.buildType.name == "release") {
					productFlavorInfo = productFlavorObject.getAsJsonObject(variant.flavorName)
				}
				
				// Publisher
				val firAppPublisher = FirAppPublisher()
				firAppPublisher.gitBranchPrefix = gitBranchPrefix
				firAppPublisher.iconFile = iconFile
				firAppPublisher.variant = variant
				firAppPublisher.apiToken = apiToken
				firAppPublisher.publicVersionInfo = versionInfo
				firAppPublisher.productFlavorInfo = productFlavorInfo
				
				// Printer
				val firAppInfoPrinter = FirAppInfoPrinter()
				firAppInfoPrinter.logger = project.logger
				firAppInfoPrinter.variant = variant
				firAppInfoPrinter.apiToken = apiToken
				firAppInfoPrinter.gitBranchPrefix = gitBranchPrefix
				firAppInfoPrinter.productFlavorInfo = productFlavorInfo
				
				// ==================== Task ====================
				
				firVariantPublisherTask.firAppPublisherList.add(firAppPublisher)
				firVariantPublisherTask.firAppInfoPrinterList.add(firAppInfoPrinter)
				firVariantPublisherTask.size++
				
				firVariantInfoPrinterTask.firAppInfoPrinterList.add(firAppInfoPrinter)
				
				firAppPublisherTask.firAppPublisherList.add(firAppPublisher)
				firAppPublisherTask.firAppInfoPrinterList.add(firAppInfoPrinter)
				firAppPublisherTask.size++
				
				firAppInfoPrinterTask.firAppInfoPrinterList.add(firAppInfoPrinter)
			}
		}
	}
}