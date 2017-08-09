package com.kycq.gradle.plugin.fir

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FirAppPublisherTask extends DefaultTask {
	boolean singlePrinter = true
	ArrayList<FirAppPublisher> firAppPublisherList = new ArrayList<>()
	ArrayList<FirAppInfoPrinter> firAppInfoPrinterList = new ArrayList<>()
	int size = 0
	
	@TaskAction
	void exec() {
		for (int index = 0; index < size; index++) {
			firAppPublisherList[index].publish(singlePrinter)
			if (singlePrinter) {
				firAppInfoPrinterList[index].printInfo(singlePrinter)
			}
		}
		if (!singlePrinter) {
			firAppInfoPrinterList.each { firAppInfoPrinter ->
				firAppInfoPrinter.printInfo(singlePrinter)
			}
		}
	}
}
