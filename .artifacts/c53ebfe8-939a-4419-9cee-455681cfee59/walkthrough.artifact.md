# Walkthrough - CI/CD for GitHub and GitLab Packages

I have successfully set up a dual-platform CI/CD pipeline for the `location-tracker` KMP library, allowing it to be published to both GitHub Packages and the GitLab Package Registry.

## Key Enhancements

### 1. Dual-Registry Publishing Support
- **SDK Build Script**: Updated [location-tracker/build.gradle.kts](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker/build.gradle.kts) to define both `GitHubPackages` and `GitLab` repositories in the `publishing` block.
- **Configurability**: Integrated project properties like `GITLAB_PROJECT_ID` and `GITLAB_API_URL` to allow dynamic publishing coordinates without hardcoding values in the build script.

### 2. GitHub Actions Workflow
- **File**: [.github/workflows/publish.yml](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.github/workflows/publish.yml)
- **Features**:
    - **Automated Checks**: Runs library tests and builds the XCFramework on every push to `main` and pull requests.
    - **Tag-Triggered Release**: Automatically publishes to GitHub Packages whenever a version tag (e.g., `v0.1.1`) is pushed.
    - **macOS Runners**: Uses `macos-latest` to ensure all multiplatform targets (including iOS) are built correctly.

### 3. GitLab CI Pipeline
- **File**: [.gitlab-ci.yml](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.gitlab-ci.yml)
- **Features**:
    - **Multi-stage Pipeline**: Defines `test`, `build`, and `publish` stages.
    - **Native Artifacts**: Builds and archives the iOS XCFramework as a CI artifact.
    - **Secure Publishing**: Uses `CI_JOB_TOKEN` for seamless authentication with the GitLab Package Registry.

### 4. Comprehensive Setup Guide
- **Updated README**: The root [README.md](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/README.md) now includes a dedicated "CI/CD & Publishing" section.
- **Consumption Instructions**: Provided clear Gradle snippets for developers to consume the library from either GitHub or GitLab, including authentication requirements.

## Verification
- **Build Success**: Confirmed that `./gradlew :location-tracker:assemble` runs successfully with the new publishing configuration.
- **Test Integrity**: Verified that existing library tests pass on both Android and iOS targets.

> [!IMPORTANT]
> To finish the setup, ensure you add the following CI/CD variables in your GitLab project settings if you haven't already:
> - `GITLAB_PROJECT_ID`: Your numeric GitLab project ID.
> - `GITLAB_API_URL`: Usually `https://gitlab.com/api/v4`.
