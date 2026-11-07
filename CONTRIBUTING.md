# Contributing

This guide provides instructions for contributing to this Capacitor plugin.

## Developing

### Local Setup

1. Fork and clone the repo.
1. Install the dependencies.

    ```shell
    npm install
    ```

1. Install SwiftLint if you're on macOS.

    ```shell
    brew install swiftlint
    ```

### Scripts

#### `npm run build`

Build the plugin web assets and generate plugin API documentation using [`@capacitor/docgen`](https://github.com/ionic-team/capacitor-docgen).

It will compile the TypeScript code from `src/` into ESM JavaScript in `dist/esm/`. These files are used in apps with bundlers when your plugin is imported.

Then, Rollup will bundle the code into a single file at `dist/plugin.js`. This file is used in apps without bundlers by including it as a script in `index.html`.

#### `npm run verify`

Build and validate the web and native projects.

This is useful to run in CI to verify that the plugin builds for all platforms.

#### `npm run lint` / `npm run fmt`

Check formatting and code quality, autoformat/autofix if possible.

This template is integrated with ESLint, Prettier, and SwiftLint. Using these tools is completely optional, but the [Capacitor Community](https://github.com/capacitor-community/) strives to have consistent code style and structure for easier cooperation.

## Publishing

For the Android reminder change in 0.0.2, run `gradlew.bat assembleRelease
testDebugUnitTest` from `android/` on Windows (or `./gradlew` on macOS/Linux).
With an emulator or test device connected, run `gradlew.bat
connectedDebugAndroidTest`. `ReminderNotificationTest` checks the merged test
manifest, the audible channel migration, notification flags, dismissal,
cancellation, and scheduling without exact access. The fallback test requires
Android 12+ with exact-alarm access disabled; it skips if access is already granted.

Also manually test the permission prompts, a reminder delivered with the app in
the background, sound playing once, Dismiss/swipe-away, and a reminder scheduled
after refusing exact access. For upgrades, install 0.0.1 first so its silent
notification channel exists, then update the test app to 0.0.2. Confirm the new
reminder channel sounds. Inspect the consuming app's final merged manifest or AAB
for the removed permissions and playback service before submitting it to Play.

There is a `prepublishOnly` hook in `package.json` which prepares the plugin before publishing, so all you need to do is run:

```shell
npm publish
```

> **Note**: The [`files`](https://docs.npmjs.com/cli/v7/configuring-npm/package-json#files) array in `package.json` specifies which files get published. If you rename files/directories or add files elsewhere, you may need to update it.
