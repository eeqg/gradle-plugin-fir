package com.kycq.gradle.plugin.fir

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

open class JenkinsJobTask : DefaultTask() {
	companion object {
		val JOBS_CONFIG = "<project>\n" +
				"    <actions/>\n" +
				"    <description/>\n" +
				"    <keepDependencies>false</keepDependencies>\n" +
				"    <properties/>\n" +
				"    <scm class=\"hudson.plugins.git.GitSCM\" plugin=\"git@3.6.4\">\n" +
				"        <configVersion>2</configVersion>\n" +
				"        <userRemoteConfigs>\n" +
				"            <hudson.plugins.git.UserRemoteConfig>\n" +
				"                <url>@{configGitUrl}</url>\n" +
				"                <credentialsId>@{configCredentialsId}</credentialsId>\n" +
				"            </hudson.plugins.git.UserRemoteConfig>\n" +
				"        </userRemoteConfigs>\n" +
				"        <branches>\n" +
				"            <hudson.plugins.git.BranchSpec>\n" +
				"                <name>*/@{configBranch}</name>\n" +
				"            </hudson.plugins.git.BranchSpec>\n" +
				"        </branches>\n" +
				"        <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>\n" +
				"        <submoduleCfg class=\"list\"/>\n" +
				"        <extensions/>\n" +
				"    </scm>\n" +
				"<canRoam>true</canRoam>\n" +
				"<disabled>false</disabled>\n" +
				"<blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n" +
				"<blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n" +
				"<triggers/>\n" +
				"<concurrentBuild>false</concurrentBuild>\n" +
				"<builders>\n" +
				"    <hudson.plugins.gradle.Gradle plugin=\"gradle@1.28\">\n" +
				"    <switches/>\n" +
				"    <tasks>@{configTask}</tasks>\n" +
				"    <rootBuildScriptDir/>\n" +
				"    <buildFile/>\n" +
				"    <gradleName>@{jenkinsTaskGradleName}</gradleName>\n" +
				"    <useWrapper>false</useWrapper>\n" +
				"    <makeExecutable>false</makeExecutable>\n" +
				"    <useWorkspaceAsHome>false</useWorkspaceAsHome>\n" +
				"    <wrapperLocation/>\n" +
				"    <passAllAsSystemProperties>false</passAllAsSystemProperties>\n" +
				"    <projectProperties/>\n" +
				"    <passAllAsProjectProperties>false</passAllAsProjectProperties>\n" +
				"    </hudson.plugins.gradle.Gradle>\n" +
				"</builders>\n" +
				"<publishers/>\n" +
				"<buildWrappers/>\n" +
				"</project>\n"
	}
	
	lateinit var gitUrl: String
	lateinit var gitBranch: String
	lateinit var jobName: String
	
	lateinit var jenkinsUrl: String
	lateinit var jenkinsAuthrization: String
	lateinit var jenkinsCredentialsId: String
	lateinit var jenkinsTaskName: String
	lateinit var jenkinsTaskGradleName: String
	
	@TaskAction
	fun exec() {
		val url = URL("$jenkinsUrl/createItem?name=$jobName")
		val httpURLConnection = url.openConnection() as HttpURLConnection
		httpURLConnection.requestMethod = "POST"
		httpURLConnection.connectTimeout = 10000
		httpURLConnection.readTimeout = 2000
		httpURLConnection.doOutput = true
		httpURLConnection.doInput = true
		
		httpURLConnection.setRequestProperty("Content-Type", "application/xml")
		httpURLConnection.setRequestProperty("Authorization", "Basic $jenkinsAuthrization")
		
		val configContent = JOBS_CONFIG
				.replace("@{configGitUrl}", this.gitUrl)
				.replace("@{configCredentialsId}", this.jenkinsCredentialsId)
				.replace("@{configBranch}", this.gitBranch)
				.replace("@{configTask}", this.jenkinsTaskName)
				.replace("@{jenkinsTaskGradleName}", this.jenkinsTaskGradleName)
		
		val dataOutputStream = DataOutputStream(httpURLConnection.outputStream)
		dataOutputStream.write(configContent.toByteArray(Charset.forName("UTF-8")))
		dataOutputStream.flush()
		dataOutputStream.close()
		
		if (httpURLConnection.responseCode != 200) {
			val reader = BufferedReader(InputStreamReader(httpURLConnection.errorStream))
			val errorPattern = Pattern.compile(".*<p>([\\s\\S]*?)</p>.*")
			val errorMatcher = errorPattern.matcher(reader.readText())
			if (errorMatcher.find()) {
				throw RuntimeException(errorMatcher.group(1))
			} else {
				throw RuntimeException("failure")
			}
		}
	}
}