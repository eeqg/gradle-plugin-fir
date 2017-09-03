package com.kycq.gradle.plugin.fir

import com.android.build.gradle.AppExtension
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.Plugin
import org.gradle.api.Project

open class FirPlugin : Plugin<Project> {
	private val FIR_APP_EXTENSION = "firPublisher"
	private val API_TOKEN = "apiToken"
	private val VERSION_INFO = "versionInfo"
	private val PRODUCT_FLAVORS = "productFlavors"
	private val ASSEMBLE = "assemble"
	private val GROUP_NAME = "publish"
	
	override fun apply(project: Project) {
		project.extensions.create(FIR_APP_EXTENSION, FirAppExtension::class.java)
		
		project.afterEvaluate {
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