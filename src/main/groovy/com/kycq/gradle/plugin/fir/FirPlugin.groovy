package com.kycq.gradle.plugin.fir

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project

class FirPlugin implements Plugin<Project> {
	static String GROUP = "publish"
	
	@Override
	void apply(Project project) {
		project.extensions.create('firPublisher', FirAppExtension)
		
		project.afterEvaluate {
			def infoFile = project.firPublisher.infoFile
			if (infoFile == null) {
				throw new NullPointerException("must config firPublisher.infoFile it build.gradle.")
			}
			def jsonInfo = new JsonSlurper().parseText(infoFile.text)
			def apiToken = jsonInfo.apiToken
			if (apiToken == null) {
				throw new NullPointerException("must config apiToken at jsonRoot in ${infoFile} file.")
			}
			def versionInfo = jsonInfo.versionInfo
			if (jsonInfo.productFlavors == null) {
				throw new NullPointerException("must config productFlavors at JsonRoot in ${infoFile} file.")
			}
			
			def iconFile = project.firPublisher.iconFile
			
			def firAppPublisherTask = project.tasks.create("publishFir", FirAppPublisherTask)
			firAppPublisherTask.dependsOn(project.assemble)
			firAppPublisherTask.group = GROUP
			firAppPublisherTask.singlePrinter = false
			
			def firAppInfoPrinterTask = project.tasks.create("printFir", FirAppPrinterTask)
			firAppInfoPrinterTask.group = GROUP
			firAppInfoPrinterTask.singlePrinter = false
			
			project.android.applicationVariants.all { variant ->
				def name = variant.name.substring(0, 1).toUpperCase() + variant.name.substring(1)
				def firVariantPublisherTask = project.tasks.create("publishFir${name}", FirAppPublisherTask)
				firVariantPublisherTask.dependsOn(variant.assemble)
				firVariantPublisherTask.group = GROUP
				
				def firVariantInfoPrinterTask = project.tasks.create("printFir${name}", FirAppPrinterTask)
				firVariantInfoPrinterTask.group = GROUP
				
				def productFlavorInfo = jsonInfo.productFlavors[variant.name]
				if (productFlavorInfo == null && variant.buildType.name.equals("release")) {
					productFlavorInfo = jsonInfo.productFlavors[variant.flavorName]
				}
				
				// Publisher
				FirAppPublisher firAppPublisher = new FirAppPublisher()
				firAppPublisher.iconFile = iconFile
				firAppPublisher.variant = variant
				firAppPublisher.apiToken = apiToken
				firAppPublisher.publicVersionInfo = versionInfo
				firAppPublisher.productFlavorInfo = productFlavorInfo
				
				// Printer
				FirAppInfoPrinter firAppInfoPrinter = new FirAppInfoPrinter()
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
