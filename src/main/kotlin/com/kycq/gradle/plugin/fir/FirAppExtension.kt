package com.kycq.gradle.plugin.fir

import java.io.File

open class FirAppExtension {
	var gitUrl: String? = null
	var gitBranch: String = "master"
	
	var jenkinsUrl: String? = null
	var jenkinsAuthrization: String? = null
	var jenkinsCredentialsId: String? = null
	var jenkinsTaskGradleName: String? = null
	
	var infoFile: File? = null
	var iconFile: File? = null
}