Jenkins Ignore Committer Strategy Plugin
==========================================
This plugin provides addition configuration to prevent multi branch projects from triggering new builds based on a list of ignored email addresses.

Installation & Configuration
============================
The easiest way to install this plugin is through the plugin manager. 
Go to `Manage Jenkins > Manage Plugins > Available` and search for `Ignore committer strategy`
Once the plugin is installed, go to your job's configuration page
and under `Branch Sources` use the *Add* button next to `Build Strategies` and add `Ignore committer strategy`.
![Alt text](./plugin-add.png?raw=true "Adding build strategy")

The default behaviour is to prevent triggering new builds if at least one of the authors in the changeset
is specified in the ignore list. However, it can be changed by ticking `	Allow builds when a changeset contains non-ignored author(s)`
checkbox, in this case a new build will be triggered if changeset contains an author that is not in the exclusion list.
![Alt text](./plugin-config.png?raw=true "Configuring build strategy")

Local interactive testing
====================
In order to run this plugin locally run the following command
```bash
mvn hpi:run
```

Debugging
================
The plugin logs are available under `Manage Jenkins > System Log > All Jenkins Logs`, optionally, you can add your own log recorded to catch only plugin specific messages.
