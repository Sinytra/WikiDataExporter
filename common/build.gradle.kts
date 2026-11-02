plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
}

val mod_id: String by rootProject
val neo_form_version: String by rootProject
val metadata: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}
val render: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}
val lang: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

neoForge {
    neoFormVersion = neo_form_version
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    addModdingDependenciesTo(metadata)
    addModdingDependenciesTo(render)
    addModdingDependenciesTo(lang)

    mods {
        create(mod_id) {
            sourceSet(sourceSets.main.get())
            sourceSet(metadata)
            sourceSet(render)
            sourceSet(lang)
        }
    }
}

dependencies {
    compileOnly(group = "org.spongepowered", name = "mixin", version = "0.8.5")
    compileOnly(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.3.5")
    annotationProcessor(group = "io.github.llamalad7", name = "mixinextras-common", version = "0.3.5")

    "metadataImplementation"(sourceSets.main.get().output)
    "renderImplementation"(sourceSets.main.get().output)
    "langImplementation"(sourceSets.main.get().output)
}

configurations {
    register("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    
    register("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

tasks {
    jar {
        from(metadata.output)
        from(render.output)
        from(lang.output)
    }
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonJava", metadata.java.sourceDirectories.singleFile)
    add("commonJava", render.java.sourceDirectories.singleFile)
    add("commonJava", lang.java.sourceDirectories.singleFile)

    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
    add("commonResources", metadata.resources.sourceDirectories.singleFile)
    add("commonResources", render.resources.sourceDirectories.singleFile)
    add("commonResources", lang.resources.sourceDirectories.singleFile)
}

