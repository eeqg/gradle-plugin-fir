package com.kycq.gradle.plugin.fir

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class FirAppPublisherTask : DefaultTask() {
	var singlePrinter = true
	var firAppPublisherList : ArrayList<FirAppPublisher> = ArrayList()
	var firAppInfoPrinterList : ArrayList<FirAppInfoPrinter> = ArrayList()
	var size = 0
	
	@TaskAction
	fun exec() {
		for (index in firAppInfoPrinterList.indices) {
			firAppPublisherList[index].publish(singlePrinter)
			if (singlePrinter) {
				firAppInfoPrinterList[index].printInfo(singlePrinter)
			}
		}
		if (!singlePrinter) {
			firAppInfoPrinterList.forEach { firAppInfoPrinter ->
				firAppInfoPrinter.printInfo(singlePrinter)
			}
		}
	}
}