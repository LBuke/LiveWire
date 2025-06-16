# LiveWire

**LiveWire** is a [PaperMC](https://papermc.io/) plugin designed to make plugin development easier by enabling **hot-reloading** of plugins on your Minecraft server.

When you upload a `.jar` to a special folder, LiveWire detects the change and automatically handles unloading the old version and loading the new one — no server restart required.

---

## ✨ Features

- 📂 Creates a `/livewire` directory in the server root
- 🔄 Watches the directory for new or updated `.jar` files
- 🚀 Automatically hot-reloads plugins when `.jar`s are added or replaced
- 🚫 Supports configurable regex-based ignore patterns for specific plugins

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

### `config.yml`

```yaml
ignore:
  - "^SomePlugin.*"
  - "testplugin"
```
