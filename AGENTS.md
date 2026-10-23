# Project layout

Keep OVRPort source, builds, release packages and diagnostics inside this project.
Use `dist/releases/<version>/` for local release packages and `dist/archive/` for retained old test builds; both are ignored by Git.
Never put the patcher application, its JRE or its ZIP into a game project. Game APKs and game-specific diagnostics belong in their respective game projects or disposable local test directories, never release assets.
After a published version, assign the next beta number to new test builds. Do not publish without the user's authorization. Keep release notes short, without game lists or instructions.
