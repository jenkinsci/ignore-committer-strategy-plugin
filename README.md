# Ignore Committer Strategy Plugin

This plugin provides additional configuration to prevent multi-branch projects from triggering new builds based on a list of ignored email addresses.

## Configuration

Once the plugin is installed, go to the job configuration page of the multi-branch Pipeline or organization folder
and under `Branch Sources` use the *Add* button below `Build Strategies` to add `Ignore committer strategy`.

![Adding build strategy](./plugin-add.png?raw=true "Adding build strategy")

By default, builds are not triggered if at least one of the authors in the changeset is specified in the ignore list.
If the `Allow builds when a changeset contains non-ignored author(s)` box is checked, a new build will be triggered if the changeset contains any author that is **not** in the exclusion list.

![Configuring build strategy](./plugin-config.png?raw=true "Configuring build strategy")
