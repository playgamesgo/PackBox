# PackBox

PackBox is a practical CLI tool that makes developing and maintaining Minecraft modpacks easier through Git integration. 
It handles mods from both Modrinth and CurseForge with automatic source detection.

## Features

- Support for multiple Minecraft mod loaders (Fabric, Forge, NeoForge, Quilt)
- Automatic detection of mods from Modrinth and CurseForge
- Installing mods based on a manifest file

## Installation

### Github Releases

Download the latest release from the [Releases page](https://github.com/playgamesgo/PackBox/releases).


### Building from Source

Requirements:
- GraalVM JDK 21 or later
- Gradle

```bash
# Clone the repository
git clone https://github.com/playgamesgo/PackBox.git
cd PackBox

# Build JAR and native binary
./gradlew build shadowJar nativeCompile
```

The compiled native binary will be located at `build/native/nativeCompile`.

## Usage

### Initialize a New Modpack

```bash
packbox init
```

Options:
- `-n, --name` - Name of the modpack
- `-a, --author` - Author of the modpack
- `-v, --version` - Version of the modpack
- `-l, --loader` - Mod loader (fabric, forge, quilt, neoforge)
- `-m, --minecraft` - Minecraft version
- `-lv, --loader-version` - Loader version
- `-s, --source` - Primary mod source (Modrinth, Curseforge)
- `-f, --fallback` - Fallback mod source (None, or the other source)

### Detect Mods

Scans directories for mods and automatically identifies their sources:

```bash
packbox detect
```

Options:
- `-f, --folders` - Folders to detect mods from, comma separated (mods, resourcepacks, shaderpacks)

### Install Mods

Installs all mods defined in the manifest file:

```bash
packbox install
```

## Workflow Example

```bash
# Initialize a new modpack
packbox init -n "My Awesome Pack" -a "YourName" -l fabric -m 1.20.1 -lv 0.16.3 -s Modrinth

# Add your mods to the mods directory

# Detect all mods in the mods directory
packbox detect -f mods

# Share your modpack (push to git)
git init
git add manifest.packbox.json
git commit -m "Initial modpack setup"
git push

# Install the modpack
git clone your-repository
cd your-repository
packbox install
```

## Manifest File

The `manifest.packbox.json` file contains all the information about your modpack, including:

- Basic information (name, author, version)
- Minecraft and loader versions
- List of mods with their sources, URLs, and checksums

This file should be committed to your git repository.

## Building

This project uses Gradle and GraalVM native image for building. A GitHub Actions workflow is set up to build both JAR and native binaries for multiple platforms.

## Support

Need help? Join our [Discord server](https://discord.gg/AFrDuzEre6) for support and community discussions.

## License

This project is licensed under the GPL-3.0 License — see the LICENSE file for details.

## Contributing

Contributions are welcome! Feel free to submit issues or pull requests.