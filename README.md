# Web-wrapper for Jellyfin Android TV

[![License: GPL v2](https://img.shields.io/badge/License-GPL_v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

A simple Jellyfin web-wrapper project for android tv

## Project Progress for now

- [x] Project setup and basic configuration
- [x] WebView implementation for Jellyfin web client
- [x] Custom user agent for TV compatibility
- [x] Basic navigation controls for TV remote
- [x] URL input screen with validation
- [x] Server URL caching using SharedPreferences
- [ ] Full remote control support
- [ ] Hardware acceleration optimization
- [ ] Error handling and user feedback
- [ ] Custom TV-optimized UI components
- [ ] Settings screen for configuration

## Requirements
- Android Studio Giraffe (2022.3.1+)
- Android SDK (API 35)
- OpenJDK 21+

## Build Instructions

```bash
# Clone repository
git clone https://github.com/Sam42a/DUNE-WEB.git
cd DUNE-WEB

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```
## License

This project is licensed under the GNU General Public License v2.0 - see the [LICENSE](LICENSE) file for details.