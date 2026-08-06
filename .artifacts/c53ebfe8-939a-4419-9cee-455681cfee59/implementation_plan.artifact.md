# Implementation Plan - CI/CD for GitHub and GitLab Packages

Set up automated pipelines to test, build, and publish the `location-tracker` KMP library to both GitHub Packages and GitLab Package Registry.

## User Review Required

> [!IMPORTANT]
> **Secrets and Variables Configuration**
> - **GitHub**: You must configure `GPR_USER` and `GPR_TOKEN` (with `write:packages` scope) in your GitHub Repository Secrets if the default `GITHUB_TOKEN` is insufficient.
> - **GitLab**: You must provide `GITLAB_PROJECT_ID` in your CI/CD variables. `CI_JOB_TOKEN` is usually available by default for internal registry access.
> - **iOS Builds**: Both pipelines will use macOS runners to ensure iOS targets can be built and packaged into XCFrameworks.

## Proposed Changes

### [location-tracker](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker)

#### [MODIFY] [build.gradle.kts](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/location-tracker/build.gradle.kts)
- Update the `publishing` block to include a GitLab repository.
- Use flexible property lookups for GitLab-specific settings (`GITLAB_PROJECT_ID`, `GITLAB_API_URL`).

### [root](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP)

#### [MODIFY] [gradle.properties](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/gradle.properties)
- Add placeholder properties for GitLab publishing to prevent build failures.

#### [NEW] [.github/workflows/publish.yml](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.github/workflows/publish.yml)
- Define a workflow that runs on `push` to `main` (tests/build) and on `tags` (publish).
- Use `macos-latest` to support all KMP targets.
- Tasks: `check`, `assemble`, `publishAllPublicationsToGitHubPackagesRepository`.

#### [NEW] [.gitlab-ci.yml](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/.gitlab-ci.yml)
- Define a GitLab pipeline with `test`, `build`, and `publish` stages.
- Use a macOS runner tag (if available in your GitLab environment) or split into Linux (Android) and macOS (iOS) jobs.
- Task: `publishAllPublicationsToGitLabRepository`.

#### [MODIFY] [README.md](file:///Users/vianet/Kabindra/Own/Location-Tracker-KMP/README.md)
- Update the "Setup" section to provide instructions for both GitHub and GitLab dependency consumption.

## Verification Plan

### Automated Tests
- The CI pipelines themselves will verify the project's health by running `./gradlew check`.

### Manual Verification
- Trigger a trial run of the GitHub Action to verify it can successfully build the XCFrameworks.
- Verify the GitLab CI configuration syntax using the GitLab Lint tool.
- Inspect the generated POM files to ensure group and artifact IDs are consistent across registries.
