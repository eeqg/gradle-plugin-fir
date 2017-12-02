package com.kycq.gradle.plugin.fir

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class FirAppPrinterTask : DefaultTask() {
	var singlePrinter = true
	var firAppInfoPrinterList: ArrayList<FirAppInfoPrinter> = ArrayList()
	
	@TaskAction
	fun exec() {
		firAppInfoPrinterList.forEach { firAppInfoPrinter ->
			firAppInfoPrinter.printInfo(singlePrinter)
		}
	}
}