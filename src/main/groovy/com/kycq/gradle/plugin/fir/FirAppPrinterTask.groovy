package com.kycq.gradle.plugin.fir

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class FirAppPrinterTask extends DefaultTask {
	boolean singlePrinter = true
	ArrayList<FirAppInfoPrinter> firAppInfoPrinterList = new ArrayList<>()
	
	@TaskAction
	void exec() {
		firAppInfoPrinterList.each { firAppInfoPrinter ->
			firAppInfoPrinter.printInfo(singlePrinter)
		}
	}
}
