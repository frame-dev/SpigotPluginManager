# Spigot Plugin Manager

A simple Spigot plugin manager to help manage Spigot server plugins locally or on remote servers via SSH. This project uses the Java Swing framework for the GUI (Desktop Environment required).

## Features

- **Install plugins** from local files or URLs
- **Uninstall plugins** with a single click
- **Enable or disable plugins** by renaming them with a configurable suffix
- **Manage plugins on remote servers** over SSH (upload, enable/disable, uninstall, list)
- **Customizable settings** for local and remote plugin management
- **User-friendly graphical interface**
- **Display Plugin Information** such as name, version, description, authors and more

## Requirements

- Java 21 or higher
- Spigot server version 1.16 or higher (local or remote)
- Internet connection for downloading plugins from URLs
- For remote usage: SSH access to the remote server (password or key-based authentication)

## Installation

1. Download the latest version from the [releases page](https://github.com/frame-dev/SpigotPluginManager/releases).
2. Run the application with `java -jar SpigotPluginManager-1.2-SNAPSHOT.jar`
3. Select your Spigot server's `plugins` folder from the **File** menu for local management, or configure a remote SSH target in **Settings** for remote management.
4. Manage your plugins using the provided buttons.

## Usage

1. **Select Plugin Folder (local)**: Go to `File > Select Plugin Folder` and choose your server's `plugins` directory.
2. **Configure Remote (SSH)**: Open `File > Settings` and add a remote target:
   - **Host** — remote server hostname or IP
   - **Port** — SSH port (default 22)
   - **Username** — SSH user
   - **Authentication** — password or path to private key
   - **Remote plugin path** — path to the `plugins` folder on the remote server
   - Use the test connection button to verify access
3. **Install**:
   - **Local**: Click `Install...` to add a plugin from your local disk
   - **URL**: Click `Install from URL...` to download a plugin and install it to the selected local or remote `plugins` folder
   - **Remote**: When connected to a remote target, installers upload the plugin over SSH and place it in the remote `plugins` folder
4. **Enable/Disable**: Select a plugin and click `Enable` or `Disable` to toggle its state (local rename or remote rename via SSH).
5. **Uninstall**: Select an installed plugin and click `Uninstall` to remove it locally or remotely.
6. **Refresh**: Click `Refresh` to reload the plugin lists from the selected target (local or remote).

## Settings

Access settings via `File > Settings`:

| Option | Description |
|--------|-------------|
| Start with latest folder | Automatically load the last selected plugin directory on startup |
| Suffix for disabled plugins | The file extension used for disabled plugins (default: `.disabled`) |
| Remote targets | Add/manage remote SSH targets (host, port, username, auth, remote plugin path) |
| Default remote plugin path | Default path to the `plugins` folder used when adding new remote targets |

**Security note:** When using SSH key authentication, ensure private keys have correct permissions and are stored securely. Passwords and keys are saved only if enabled in settings and should be protected by your OS user account.

## Building from Source

1. Clone the repository: `git clone https://github.com/frame-dev/SpigotPluginManager.git`
2. Navigate to the directory: `cd SpigotPluginManager`
3. Build with Maven: `mvn clean package`
4. The built JAR will be in the `target` directory.

## Screenshots

![Screenshot](screenshots/screenshot_1.png)

![Screenshot](screenshots/screenshot_2.png)

![Screenshot](screenshots/screenshot_3.png)

## License

This project is open source. See the [LICENSE](LICENSE) file for details.

## Author

Developed by [FrameDev](https://github.com/frame-dev)