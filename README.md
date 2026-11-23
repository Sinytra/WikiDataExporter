# Wiki Data Exporter

A minimal mod to render and export item assets and other useful information from mods.

Made for [Modded Minecraft Wiki](https://moddedmc.wiki) authors. Use in combination with the wiki
[Gradle plugin](https://github.com/Sinytra/WikiToolkit) to configure automatically and export everything in one click.

## Installation

```groovy
repositories {
    maven {
        url "https://maven.sinytra.org"
    }
}

dependencies {
    // ModDevGradle
    // Version list: https://maven.sinytra.org/org/sinytra/wiki-exporter-neoforge
    runtimeOnly "org.sinytra:wiki-exporter-neoforge:<version>"

    // Fabric Loom
    // Version list: https://maven.sinytra.org/org/sinytra/wiki-exporter-fabric
    modRuntimeOnly "org.sinytra:wiki-exporter-fabric:<version>"
}
```

<details>
<summary>See instructions for 1.21.4 and below</summary>

```groovy
dependencies {
    // ModDevGradle
    // Version list: https://maven.sinytra.org/org/sinytra/item-asset-export-neoforge
    runtimeOnly "org.sinytra:item-asset-export-neoforge:<version>"

    // Fabric Loom
    // Version list: https://maven.sinytra.org/org/sinytra/item-asset-export-fabric
    modRuntimeOnly "org.sinytra:item-asset-export-fabric:<version>"
}
```
</details>

## Usage

### Configure the exporter

The exporter is configured using a JSON file following the format described in `config.example.jsonc`.
When copying this file, please remove the comments and use the `.json` extension instead.

By default, the exporter will look for the configuration file at `run/wiki_exporter/config.json`, but you can define
your own file path using the `wiki_exporter.config.path` system property. See below for details.

If no configuration file is found, the exporter uses a default configuration that enables all modules and exports
content from the mod you're developing.

Defaults for the `render` module are 128x128 PNG images.

Example configuration:

```json5
{
  // (optional) Output base path, relative to the "run" directory
  "output_path": "wiki_exporter",
  // Enabled exporter modules
  "enabled": [
    "metadata",
    "lang",
    "render"
  ],
  // Per-module config
  "modules": {
    "render": {
      // In-game ID namespaces to export from
      "namespaces": [
        "minecraft"
      ],
      // Export file resolution in px
      "resolution": 128,
      // Enable static texture exports
      "png": true
    }
  }
}
```

### Enable the exporter

The exporter will only run when the `wiki_exporter.enabled` system property is set to `true`.
You should configure it individually per each run configuration as desired.
We recommend creating a separate run configuration for the exporter.

The exporter will run automatically when the game launches, and will close it once all exports are complete.

All modules can run on the client. Once the game is loaded, the exporter loads server data without creating a world,
then runs enabled modules and exits the game.

Example configuration using NeoForge with ModDevGradle:

```groovy
neoForge {
    runs {
        exportClient {
            client()
            systemProperty 'wiki_exporter.enabled', 'true'
        }
    }
}
```

The exporter can also run on a dedicated server, in which case only modules that support the server side will run
(see table below).

```groovy
neoForge {
    runs {
        exportServer {
            server()
            programArgument 'nogui'
            systemProperty 'wiki_exporter.enabled', 'true'
        }
    }
}
```

## Reference

### Available modules

| ID         | Description                                                                           | Side   |
|------------|---------------------------------------------------------------------------------------|--------|
| `render`   | Exports GUI renders of specified items/blocks                                         | CLIENT |
| `metadata` | Exports item and block properties such as stack size, hardness, blast resistance etc. | BOTH   |
| `lang`     | Exports all loaded language files                                                     | CLIENT |

### Available system properties

| Name                                      | Description                             | Default value                     |
|-------------------------------------------|-----------------------------------------|-----------------------------------|
| `wiki_exporter.enabled`                   | Enable exporter                         | `false`                           |
| `wiki_exporter.config.path`               | Path to exporter configuration file.    | `<run>/wiki_exporter/config.json` |
| `wiki_exporter.output.path`               | Path to exporter base output directory. | `<run>/wiki_exporter/output`      |
| `wiki_exporter.module.<name>.output.path` | Per-module output directory override.   | -                                 |
