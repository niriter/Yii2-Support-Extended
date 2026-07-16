# Repository Instructions for Codex Agents

## Release requirements

Whenever a task creates, prepares, retries, or publishes a new plugin release, the agent must complete the following checklist before creating or pushing the release tag.

1. Determine the release version from the intended Git tag. A tag such as `v1.2.3` produces plugin version `1.2.3`.
2. Add a section for that exact version and the current release date near the top of `CHANGELOG.md`.
3. Update `<change-notes>` in `resources/META-INF/plugin.xml` for that exact version.
4. Keep `<change-notes>` limited to the current release. JetBrains Marketplace preserves notes for older uploaded versions, so old release notes do not need to remain in `plugin.xml`.
5. Describe user-visible changes clearly. If the release contains only build, signing, packaging, or CI changes, explicitly state that there are no plugin functionality changes.
6. Never reuse stale notes or leave a version number in `<change-notes>` that differs from the version being released.

Before creating or pushing the tag, validate the release metadata:

```bash
xmllint --noout resources/META-INF/plugin.xml
./gradlew -PpluginVersion=<version-without-v> patchPluginXml
```

Then inspect `build/tmp/patchPluginXml/plugin.xml` and confirm that:

- `<version>` equals the tag version without the `v` prefix;
- `<change-notes>` describes the same version;
- `CHANGELOG.md` contains the same version and release date.

The release workflow must build, sign, and verify the plugin with `signPlugin` and `verifyPluginSignature`, and it must attach the resulting `*-signed.zip` archive to the GitHub Release. After the workflow completes, confirm that the release asset is signed and that its checksum matches the GitHub asset digest.

Do not move or recreate an existing published tag to correct release metadata. Prepare the next version instead unless the user explicitly instructs otherwise.

Do not create commits, tags, GitHub Releases, or push changes unless the user has explicitly authorized those actions.
