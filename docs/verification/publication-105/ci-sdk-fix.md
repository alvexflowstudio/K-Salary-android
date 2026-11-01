# Publication CI SDK setup correction

The first publication run [41284164592](https://github.com/alvexflowstudio/K-Salary-android/actions/runs/41284164592) failed before app compilation/tests on merge `3121d0619678e9c9ac7fc254eb7f5578eca13952`. `android-actions/setup-android@v3` executed `sdkmanager tools`, which reported `Failed to find package 'tools'` and exited 1.

The [official action input schema](https://github.com/android-actions/setup-android/blob/v3/action.yml) sets `packages` to `tools platform-tools` by default. This workflow explicitly requests only `platform-tools`; its existing API 37 installation step is retained. No application source, dependency declaration, signing configuration, version metadata or tested APK is changed. No new local dependency is installed.

Bounded diagnosing-bugs application: the hosted run supplies the actual red signal and exact failing command. Known-cause log/action-schema comparison replaces speculative multi-hypothesis diagnosis; no local SDK-package installation or artificial unit-test seam is used. A subsequent hosted CI run is the necessary green check. No full six-phase diagnostic completion is claimed by this record before that run.
