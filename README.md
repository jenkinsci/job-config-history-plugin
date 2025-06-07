# Job Configuration History Plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins%2Fjob-config-history-plugin%2Fmaster)](https://ci.jenkins.io/job/plugins/job/job-config-history-plugin/job/master/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/jobConfigHistory.svg)](https://plugins.jenkins.io/jobConfigHistory)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/jobConfigHistory.svg?color=blue)](https://plugins.jenkins.io/jobConfigHistory)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/jobConfigHistory-plugin.svg)](https://github.com/jenkinsci/job-config-history-plugin/graphs/contributors)

This plugin saves copies of all job and system configurations, in order to maintain the history of what has changed and who made the changes.

<!-- TOC -->
* [Job Configuration History Plugin](#job-configuration-history-plugin)
  * [Documentation](#documentation)
    * [Job Config History Revision Overview](#job-config-history-revision-overview)
    * [Job Diff Side-By-Side View](#job-diff-side-by-side-view)
    * [Config Overview Page](#config-overview-page)
  * [Interdependencies with other Plugins or Excluding config path parts such as jobs, folders](#interdependencies-with-other-plugins-or-excluding-config-path-parts-such-as-jobs-folders)
  * [Open Issues](#open-issues)
  * [Changelog](#changelog)
  * [Notes](#notes)
<!-- TOC -->

------------------------------------------------------------------------

## Documentation

#### Job Config History Revision Overview

This plugin saves **a copy of the configuration file** of jobs and agents (`config.xml`) for every change made and of the system configuration (`<config-name>.xml`).
You can also see what changes have been made by which user if you configured a security policy.

![](docs/img/Job_Config_History_Revision_Overview.png)

#### Job Diff Side-By-Side View

It is also possible to get a **side-by-side view** of the differences between two configurations and to restore an old version of a job's configuration. (The latter is only available for jobs, not for system changes.)
However, if you restore an older version of the config file and the new version contains fields that were not present in the older version, the restored version will still contain these fields, although they were not present in the original.

![](docs/img/Job_Diff_Side-By-Side_View.png) 

#### Config Overview Page

The plugin also provides an **overview page** of all changes. You can find it under `<jenkins_url>/jobConfigHistory` or reach it via links in the sidepanel of the main and the system configuration pages.
The overview page initially only lists system configuration changes (for performance reasons), but there are links to view either all job configuration histories or just the deleted jobs or all kinds of configuration history entries together (which may take some time to load, depending on the number of jobs in your instance).

![](docs/img/Config_Overview_Page.png)


------------------------------------------------------------------------

## Interdependencies with other Plugins or Excluding config path parts such as jobs, folders

If you use other plugins, that are (automatically) changing the configuration, you might end up with a lot of unwanted change detections.

It's recommended to use the given ability, to exclude such changes by patterns. This can be done in the System Configuration or via `<jenkins-link>/configure`. Just add the corresponding pattern (e.g. cluster-stats) in the input **Configuration exclude file pattern**:

![](docs/img/globalconfig.png)

This functionality also allows you to exclude jobs, folders or other parts of your job hierarchy:
The pattern is attempted to be found in a config file's file path. Adding `|test` to the pattern will also cause `testFolder/jobs/job1` to be excluded.
So if you want to exclude jobs, only, it might be better to enter something like `|testJob/config\.xml`.

If you use this plugin together with the [Global Build Stats Plugin](https://plugins.jenkins.io/global-build-stats/) you should add `global-build-stats` to the excludes in the **Configuration exclude file pattern** section of this plugin in **Configure System** page. Fixed in version 1.9 of the plugin.

If you use this plugin together with the [Cluster Statistics Plugin](https://plugins.jenkins.io/cluster-stats/) you should add `cluster-stats` to the excludes in the **Configuration exclude file pattern** section of this plugin in **Configure System** page. Otherwise it might exceed your storage.

------------------------------------------------------------------------

## Open Issues

* See [open issues](https://issues.jenkins.io/issues/?jql=resolution%20is%20EMPTY%20and%20component%3D15683) on issues.jenkins-ci.org.

------------------------------------------------------------------------

## Changelog

See
[Changelog](https://github.com/jenkinsci/job-config-history-plugin/releases)
on Github.

------------------------------------------------------------------------
## Notes

* This plugin uses *highlight.js* for syntax highlighting (code and diffs). See
    + [github](https://github.com/highlightjs/highlight.js/)
    + [homepage](https://highlightjs.org/)
    
* Development: Make `hpi:run` work with shared libraries
    * install shared library plugin (if not installed already)
    * install git (if you want to use the shared library plugin with git)
