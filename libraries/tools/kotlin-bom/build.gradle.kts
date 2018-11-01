
description = "Kotlin Libraries bill-of-materials (BOM)"

configurations {
    create("archives")
}

artifacts {
    add("archives", file("kotlin-bom.pom"))
}

publish {
    val pom: MavenPom by extra

    pom.project {
        withGroovyBuilder {
            "packaging"("pom")
            "dependencyManagement" {
                "dependencies" {
                    for (library in listOf(
                        "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-stdlib-js", "kotlin-stdlib-common",
                        "kotlin-reflect",
                        "kotlin-osgi-bundle",
                        "kotlin-test", "kotlin-test-junit", "kotlin-test-junit5", "kotlin-test-testng", "kotlin-test-js",
                        "kotlin-test-common", "kotlin-test-annotations-common",
                        "kotlin-main-kts",
                        "kotlin-script-runtime", "kotlin-script-util",
                        "kotlin-scripting-common", "kotlin-scripting-jvm", "kotlin-scripting-jvm-host",
                        "kotlin-compiler",
                        "kotlin-daemon-client"
                    ))
                    "dependency" {
                        "groupId"(project.group)
                        "artifactId"(library)
                        "version"(project.version)
                    }
                }
            }
        }
    }
}