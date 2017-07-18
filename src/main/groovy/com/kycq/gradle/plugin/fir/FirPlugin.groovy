package com.kycq.gradle.plugin.fir

import com.android.build.gradle.api.ApkVariantOutput
import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project

class FirPlugin implements Plugin<Project> {
	static String GROUP = "publish"
	
	@Override
	void apply(Project project) {
		project.extensions.create('firPublisher', FirPublisherExtension)
		
		def publishOutputInfo = []
		
		def firPublishQuiteTask = project.tasks.create("publishFirQuite") {
			doLast {
				FirApkInfoTask.IS_PRINT_INFO = false
			}
		}
		firPublishQuiteTask.group = GROUP
		def firPublishNoisyTask = project.tasks.create("publishFirNoisy") {
			doLast {
				FirApkInfoTask.IS_PRINT_INFO = true
			}
		}
		firPublishNoisyTask.group = GROUP
		def firPublisherTask = project.tasks.create("publishFir") {
			doLast {
				publishOutputInfo.each { variantFirPublisherTask ->
					variantFirPublisherTask.firApkInfoTask.printInfo()
				}
			}
		}
		firPublisherTask.group = GROUP
		
		firPublisherTask.dependsOn(firPublishQuiteTask)
		firPublisherTask.dependsOn(firPublishNoisyTask)
		firPublishNoisyTask.mustRunAfter(firPublishQuiteTask)
		
		project.android.applicationVariants.all { variant ->
			if (variant.buildType.debuggable) {
				return
			}
			if (!variant.signingReady) {
				throw new RuntimeException("must add signingConfigs.")
			}
			
			def infoFile = project.firPublisher.infoFile
			if (infoFile == null) {
				throw new NullPointerException("must config firPublisher.infoFile it build.gradle.")
			}
			def iconFile = project.firPublisher.iconFile
			def jsonInfo = new JsonSlurper().parseText(infoFile.text)
			def apiToken = jsonInfo.apiToken
			if (apiToken == null) {
				throw new NullPointerException("must config apiToken at jsonRoot in ${infoFile} file.")
			}
			if (jsonInfo.productFlavors == null) {
				throw new NullPointerException("must config productFlavors at JsonRoot in ${infoFile} file.")
			}
			
			def name = variant.name.substring(0, 1).toUpperCase() + variant.name.substring(1)
			def publishTaskName = "publishFir${name}"
			
			def applicationId = variant.applicationId;
			String versionName = variant.versionName;
			def versionCode = variant.versionCode;
			
			def bundleIdSuffix
			def appName
			def changeLog
			def flavorInfo = jsonInfo.productFlavors[variant.flavorName]
			if (flavorInfo == null) {
				throw new NullPointerException("must config ${variant.flavorName} at jsonRoot.productFlavors.")
			}
			bundleIdSuffix = flavorInfo.bundleIdSuffix
			if (bundleIdSuffix == null || bundleIdSuffix.length() == 0) {
				bundleIdSuffix = ""
			} else {
				bundleIdSuffix = ".${bundleIdSuffix}"
			}
			appName = flavorInfo.appName
			if (appName == null) {
				throw new NullPointerException("must config appName at jsonRoot.productFlavors.${variant.flavorName} in ${infoFile} file.")
			}
			if (flavorInfo.versionInfo != null) {
				changeLog = flavorInfo.versionInfo[versionName]
			}
			if (changeLog == null) {
				if (jsonInfo.versionInfo == null) {
					throw new NullPointerException("must config versionInfo at json.root or jsonRoot.productFlavors.${variant.flavorName} in ${infoFile} file.")
				}
				changeLog = jsonInfo.versionInfo[versionName]
			}
			if (changeLog == null) {
				throw new NullPointerException("must config ${versionName} version log at versionInfo or jsonRoot.productFlavors.${variant.flavorName}.versionInfo in ${infoFile} file.")
			}
			
			def apkOutput = variant.outputs.find { variantOutput -> variantOutput instanceof ApkVariantOutput }
			
			def variantFirPublisherTask = project.tasks.create(publishTaskName, FirApkPublisherTask)
			variantFirPublisherTask.group = GROUP
			
			variantFirPublisherTask.apiToken = apiToken
			variantFirPublisherTask.bundleId = "${applicationId}.${variant.flavorName}.${variant.buildType.name}${bundleIdSuffix}"
			
			variantFirPublisherTask.iconFile = iconFile
			variantFirPublisherTask.apkFile = apkOutput.outputFile
			variantFirPublisherTask.appName = appName
			
			variantFirPublisherTask.versionName = versionName
			variantFirPublisherTask.versionCode = versionCode
			variantFirPublisherTask.changeLog = changeLog
			
			variantFirPublisherTask.firApkInfoTask = new FirApkInfoTask(variantFirPublisherTask.apiToken, variantFirPublisherTask.bundleId)
			
			variantFirPublisherTask.dependsOn(variant.assemble)
			
			firPublisherTask.dependsOn(variantFirPublisherTask)
			variantFirPublisherTask.mustRunAfter(firPublishQuiteTask)
			firPublishNoisyTask.mustRunAfter(variantFirPublisherTask)
			
			publishOutputInfo.add(variantFirPublisherTask)
		}
	}
}
