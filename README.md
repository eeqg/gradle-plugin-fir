# gradle-plugin-fir

## Usage

1\. config buildscript

add buildscript in root build.gradle

	buildscript {
		repositories {
			jcenter()
			// maven
			maven {
				url "https://jitpack.io"
			}
		}
		dependencies {
			...
			// gradle-plugin-fir
			classpath 'com.github.kycqdhl3c:gradle-plugin-fir:1.1.0'
		}
	}

2\. config build.gradle 

apply plugin at app module build.gradle file bottom

	apply plugin: 'com.kycq.gradle.plugin.fir'

	firPublisher {
		infoFile rootProject.file("upload.json")
		iconFile rootProject.file("upload.png")
	}

3\. config upload.json

add upload.json file at firPublisher.infoFile's path

apiToken is the fir.im api_token.

the plugin default use productFlavors‘s name for bundleId, but you can use bundleIdSuffix to append the bundleId.

you can config versionInfo for all product flavor, also you can config versionInfo one by one.

	{
		"apiToken": "",
		"versionInfo": {
			"1.0.0": "version log\n\n1.update content\n2.update content"
		},
		"productFlavors": {
			"flavorDevelop": {
				"appName": "app(develop)",
				"bundleIdSuffix": "",
				"versionInfo": {
					
				}
			},
			"flavorTest": {
				"appName": "app(test)",
				"bundleIdSuffix": "",
				"versionInfo": {
					
				}
			},
			"flavorProduct": {
				"appName": "app(product)",
				"bundleIdSuffix": "",
				"versionInfo": {
					
				}
			}
		}
	}

4.start publish

after rebuild project, you can find tasks at publish group.

the task name by publishFir${productFlavorName}.

you can use Console to publish like:

	gradle publishFirFlavorDevelopRelease