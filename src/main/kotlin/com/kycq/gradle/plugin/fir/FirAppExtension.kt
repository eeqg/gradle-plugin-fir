package com.kycq.gradle.plugin.fir

import java.io.File

open class FirAppExtension {
	var gitToolPath = "/usr/bin/git"
	
	var jenkinsUrl: String? = null
	var jenkinsAuthorization: String? = null
	var jenkinsCredentialsId: String? = null
	var jenkinsTaskGradleName: String? = null
	
	var infoFile: File? = null
	var iconFile: File? = null
}