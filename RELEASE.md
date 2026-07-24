# Maven Central Release Guide

This project uses GitHub Actions to automate releases to Maven Central through Sonatype's OSSRH (Open Source Software Repository Hosting).

## Prerequisites

Before your first release, you need to set up the required GitHub secrets and GPG signing.

### 1. Sonatype OSSRH Account

- Create a Sonatype JIRA account at https://issues.sonatype.org/
- Create a project ticket requesting namespace access for `io.github.a-n-o-d-e-r`
- Once approved, note your username and password

### 2. GPG Key Setup

Generate a GPG key (if you don't have one):

```bash
gpg --full-generate-key
```

Export your private key in base64 format:

```bash
gpg --export-secret-keys YOUR_KEY_ID | base64 > private-key.asc.b64
```

### 3. GitHub Secrets

Add these secrets to your GitHub repository settings (Settings → Secrets and variables → Actions):

- **CENTRAL_USERNAME**: Your Sonatype JIRA username
- **CENTRAL_PASSWORD**: Your Sonatype JIRA password (use an auth token for better security)
- **GPG_PRIVATE_KEY**: The base64-encoded private key from step 2
- **GPG_PASSPHRASE**: The passphrase you set for your GPG key

## Releasing

### Manual Release via GitHub Actions

1. Go to your repository's Actions tab
2. Select "Release to Maven Central" workflow
3. Click "Run workflow"
4. Enter the version number (e.g., `0.1.0`) in the input field
5. Click "Run workflow"

The workflow will:
- Update version numbers
- Create a git tag
- Build and sign artifacts
- Deploy to Maven Central staging
- Automatically release from staging to Maven Central

### Release via Command Line (Local)

If you need to release locally:

```bash
export GPG_PASSPHRASE="your-passphrase"
mvn -P ossrh release:prepare release:perform
```

## Release Process Details

The GitHub Action workflow (`release.yml`) performs the following steps:

1. **Setup**: Configures JDK 25 and Maven cache
2. **Git Configuration**: Sets up git user for commits and tags
3. **GPG Import**: Imports your GPG private key
4. **Maven Settings**: Creates Maven settings.xml with Sonatype credentials
5. **Release**: Executes Maven release goals
6. **GitHub Release**: Creates a GitHub release with release notes

## Troubleshooting

### GPG Key Issues

If the GPG key import fails:
- Ensure the private key is properly base64 encoded
- Verify the passphrase matches exactly
- Check that your key ID is correct

### Authentication Failures

- Verify OSSRH credentials in GitHub secrets
- Ensure you're using an auth token instead of your password (recommended)
- Check Sonatype JIRA for any account issues

### Release Already Staged

If a release is stuck in staging:
1. Log into https://s01.oss.sonatype.org/
2. Navigate to Staging Repositories
3. Find your repository and either Release or Drop it

## Additional Resources

- [Sonatype OSSRH Guide](https://central.sonatype.org/publishing/publish-maven/)
- [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)

## Versioning

This project follows [Semantic Versioning](https://semver.org/):
- MAJOR.MINOR.PATCH (e.g., 1.0.0)
- SNAPSHOT versions are used during development

Current version: Check `pom.xml` for the latest version.
