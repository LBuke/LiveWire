# LiveWire

**LiveWire** is a [PaperMC](https://papermc.io/) plugin designed to make plugin development easier by enabling **hot-reloading** of plugins on your Minecraft server.

When you upload a `.jar` to a special folder, LiveWire detects the change and automatically handles unloading the old version and loading the new one — no server restart required.

> [!CAUTION]
> LiveWire is designed to accelerate development workflows. While it's a powerful tool for testing and iteration, it is not recommended for use in production environments, as hot-reloading may introduce instability or unexpected behavior.


---

## 🛠️ How to Use

Once installed, using LiveWire is simple:

1. Build or obtain the .jar file for your plugin.
2. Drop the .jar into the `/livewire` folder.
3. LiveWire will automatically:
 - Detect the file
 -  Unload any previously loaded version of the plugin
 -  Load the new version

---

## 📦 Installation

1. Download the latest version of `LiveWire.jar`
2. Drop it into your server's `/plugins` folder
3. Start or restart the server
4. A `/livewire` folder will be created in your server root (next to `/plugins`)
5. Place plugin `.jar` files into `/livewire` to have them hot-reloaded

---

## ⚙️ Configuration

LiveWire includes a simple config file to ignore specific plugins by name using regular expressions.

If a dependency doesn't like being hot-reloaded, you can exclude it here.

### `config.yml`

```yaml
ignore:
  - "^SomePlugin.*"
  - "testplugin"
```
