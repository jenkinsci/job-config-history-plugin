# Changelog

## 2.26 (April 30 2020)
* [JENKINS-61049](https://issues.jenkins-ci.org/browse/JENKINS-61049) Move documentation from jenkins wiki to github
* [PR #118](https://github.com/jenkinsci/jobConfigHistory-plugin/pull/118) Simplify code
* [PR #123](https://github.com/jenkinsci/jobConfigHistory-plugin/pull/123) Remove debug lines

## 2.25 (March 13 2020)
* Remove unused HTML help files (PR #117) Thanks, darxriggs 
* Reduce deprecated API usage (PR #116) Thanks, darxriggs 
* Upgrade parent pom and dependencies (PR #115) Thanks, darxriggs 
* Fix Javadoc errors (PR #114) Thanks, darxriggs 
* (JENKINS-6659) configChange-comment
* (JENKINS-60163) Introducing paging
* (JENKINS-58601) privileges management
* (JENKINS-59812) config history provide helpful error when it can't create history
* (JENKINS-58141) Startup issues
* Fix indentation with multi-line lines.

## Version 2.24 (September 9 2019)
* (PR #96) Modernize overall design (mostly adjust it to Pipeline Configuration History).
* (PR #100) [JENKINS-58601] Allow deletion of config revisions in the respective config history overview.
* (PR #101) [JENKINS-58316] Improve error message for XmlFile::write RuntimeExceptions.
* (PR #102)
* Fix empty nodes not being displayed in diff view.
* Add xml indentation.
* Update DiffUtils.
* (PR #105) [JENKINS-55787] Switch labels from entry to checkbox (Thanks,  Unknown User (jsoref))

## Version 2.23.1 (August 6 2019)
* (PR #97) Bugfix: System configs were not persisted (regression of 8e2d9dd)

## Version 2.23 (July 23 2019)
* (PR #90) Allow user to use ignore job config history by regex. (Thanks, Unknown User (fengxx))
* (PR #91) Trim numeric form values (incl. validation) (Thanks, René Scheibe)

## Version 2.22 (June 3 2019)
* (PR #88) Code Improvements. (Thanks, René Scheibe)
* (PR #89) Init Level below COMPLETED might use SYSTEM (Thanks, Emilio Escobar )
* Version 2.21 (May 2 2019)
* (PR #87) Modernize (parent pom and Jenkins). (Thanks, René Scheibe)

## Version 2.20 (April 5 2019)
* (JENKINS-41177) Identical job configuration can have different config.xml based on edit method.
* (PR #81) Hide version changes
* (JENKINS-56023, PR #83) Display folder jobs in root history overview correctly.
* (JENKINS-45821, PR #74) Add the MilestoneStep file by default
* (JENKINS-49301, PR #84) Move job config history when a job is moved.

## Version 2.19 (December 5 2018)
* (JENKINS-53998) Job Config History plugin should not call User.current() during startup
* (PR #72) Use canonical user identify in case change initiator is unknown
* (PR #67) Fix root URL
* Findbugs fixes 
* Minor changes (Jenkinsfile, Link fixes)

## Version 2.18.3 (October 25 2018)
* (JENKINS-54223) Job configurations displayed incorrectly (raw)

## Version 2.18.2 (September 28 2018)
* (JENKINS-53819) Job configurations displayed incorrectly

## Version 2.18.1 (September 25 2018)
* Fix security issue

## Version 2.18 (September 25 2017)
* (JENKINS-47089) Wiki page for JobConfigHistory has circular link
* (PR #65) Also exclude files from the lockable resources plugin

## Version 2.17 (August 22 2017)
* (JENKINS-21600) Fixed max number of history entries issue.
* (JENKINS-19141) Make maven-plugin optional.

## Version 2.16 (April 21 2017)
* (JENKINS-42464) Job Configuration History not accessible from public IP address
* Switched unit tests from HudsonTestCase to JenkinsRule.

## Version 2.15 (July 21 2016)
* (JENKINS-34802) Exclude history collection for selected users (Note: All changes will be assigned to the next, not excluded user!)

## Version 2.14 (May 09 2016)
* (JENKINS-33641) Showing old and new job name if hovering over info image beneath operation renamed (for jobs and nodes)
* (JENKINS-25654) Fixed: Restore Project option for Job Config History gets Oops! page
* Merged pull request #49 (thanks to Brandon Koepke)
* (JENKINS-34151) Add support for Pipeline projects to display config changes in build history
* (JENKINS-30578) Error when trying to view job config history

## Version 2.13 (March 18 2016)
* (JENKINS-33289) NPE when clicking showDiffs (Jenkins Core 1.650 or higher)
* SECURITY-140 XSS vulnerability

## Version 2.12 (July 23 2015)
* (JENKINS-29063) Switch default for per-Maven modules to false (Thanks to Andrew Bayer)
* (JENKINS-24930) Alternating row bg colors (Thanks to Daniel Beck)

## Version 2.11 (April 17 2015)
* Avoid calling User.current() during Jenkins initialization (Thanks to Thomas de Grenier de Latour)
* Avoid tracking changes for cloud slave as well (Thanks to Ryan Campbel)

## Version 2.10 (November 12 2014)
* Fix proposal for NPE in ComputerHistoryListener.onConfigurationChange method (Thanks to William Bernardet)
* (JENKINS-22639) Don't record changes to AbstractCloudSlaves or Ephemeral Nodes (Thanks to Ryan Campbell and Jesse Click)

## Version 2.9 (September 02 2014)
* Fixed: Do not save config for matrix configurations (JENKINS-24412) (Thanks to Oliver Gondža)
* Fixed: Job config change not saved for maven and matrix projects (JENKINS-24410) (Thanks to Oliver Gondža)

## Version 2.8 (July 17 2014)
* Default for global configuration "saveModuleConfiguration" changed to false
* Fixed history could not saved for some job types if in the system configuration "Save folder configuration changes" wasn't set (Thanks to Jesse Glick)
* Rely on Jenkins 1.548+ so we can use TransientActionFactory (Thanks to Jesse Glick)
* Added Rest Api (JENKINS-22796) (JENKINS-22895) (JENKINS-22937) (Thanks to cfs pure))
* Fixed Building plugin in Windows environment (Testing only) (Thanks to Oleg Nenashev)
* Fixed wrong user link in diff page
* Fixed: Suppress the targetType loading issue (JENKINS-20511) (Thanks to Oleg Nenashev)

## Version 2.7 (Release failed)

## Version 2.6 (Apr 14 2014)
* Diff view: easily review changes sequentially (Next/Previous links) (JENKINS-21411)
* Show Config Versions in Diff View (JENKINS-21406)
* Add config history for slaves too (Thanks to Lucie Votypkova)
* Folder integration (JENKINS-20990) (Thanks to Jesse Glick)

## Version 2.5 (Oct 31 2013)
* Add button for restoring deleted projects
* Add view that only contains 'Created' entries
* Change purging (by quantity as well as by age) so that 'Created' entries of a project are not deleted
* Fix links so that last existing config file of a deleted job is shown (instead of an error)
* Always save system changes. (Remove configuration option for it.)
* Fix for 'Change History consumes extreme amounts of CPU' (JENKINS-17124) (Thanks to Stephan Pauxberger)
* Lazy loading bug fixed. (Thanks to Jesse Glick)

## Version 2.4 (Apr 25 2013)
* Make build badges optional (Improvement JENKINS-16793)
* Fix link to wrong config version in build badges (JENKINS-17119)
* Add traditional Chinese translations (Thanks to tan9!)
* Fix showDiffs bug that does not show all changes to the configuration file (JENKINS-17124)
* Make saving of Maven module config files optional (JENKINS-16471)
* Fix sort order that was changed by a Jenkins core bug fix for JENKINS-17039

## Version 2.3 (Feb 25 2013)
* Add option to automatically delete configuration histories based on their age (Feature request JENKINS-12233).
* Fix bug that throws NPE when building a new project (JENKINS-16496).
* Set "skip duplicate history" enabled by default (JENKINS-14303).

## Version 2.2 (Jan 30 2013)
* Catch NPE which appears when copying recently created job (Workaround for JENKINS-16499).

## Version 2.1.1 (Jan 22 2013)
* Fix bug that prevents building a job which has no config history entries.

## Version 2.1 (Jan 18 2013)
* Add badges which appear in the build history when the configuration of the respective job has changed since it was build the last time.
* Change the URL parameters so that not entire paths but just timestamps and job/system configuration names are passed (Fix for JENKINS-16375).

## Version 2.0 (Nov 19 2012)
* Save and list config info for deleted jobs (Fix for JENKINS-13069)
* Add button for restoring a previous job configuration (Fix for JENKINS-9616)
* Improve loading time of global jobConfigHistory page
* Change access control so that system changes can only be viewed by users with system configure permission
* Add support for hierachical job model (ItemGroup) (Thanks to ndeloof)

Warning

With this version the plugin changes the location where the configuration history data is stored. This means that some data might not be found by the plugin any longer and has to be moved manually. See above for more information.

With the 1.x versions of this plugin, the job configuration history data used to be stored with each job (JENKINS_HOME/jobs/JOBNAME/config-history), whereas the system configuration history was stored in an extra directory under JENKINS_HOME. As of version 2.0, both types of configuration history data are now to be found in one directory under JENKINS_HOME. The default location for this folder is JENKINS_HOME/config-history.
Now there are two possible scenarios:
If you did not use the default root history directory of the 1.x version, but set it to an individual path on the Jenkins configuration page, the 2.x versions of the plugin should still be able to find and display the data correctly.
However, if you did not change the path and used the default root history directory, your job configuration data is stored with each job, where the 2.x versions of the plugin will not find it any longer. If you want the plugin to list the job configuration history again, you have to move the data manually from JENKINS_HOME/jobs/JOBNAME/config-history to JENKINS_HOME/config-history/jobs/JOBNAME.

## Version 1.13 (Jan 31 2012)
* Fix for JENKINS-12596: Make plug-in compatible with LTS version.

## Version 1.12 (Nov 18 2011)
* Side-by-Side difference view (pull request 2).

## Version 1.11 (May 08 2011)
* pom.xml now references correct github repository.

## Version 1.10 (May 08 2011)
* Include japanese translation provided by tyuki39.
* Update to maven3 and newest plugin parent (1.409)
*Fix for JENKINS-9617: Include an easy way to see the last diff.

## Version 1.9 (Dec 20 2010)
* Workaround for JENKINS-6774, JENKINS-6943 provided by John Borghi: http 500 error thrown whilst saving a job configuration although the changes are actually persisted, Save config results in IOException: Unable to delete....config.xml when "Do not save duplicate history" is selected, mostly a Windows/NFS problem.
* Fix for JENKINS-6924: Add global-build-stats to DEFAULT_EXCLUDE in JobConfigHistoryConsts.java.
* Added Spanish translation (release #33152)

## Version 1.5 (May 31 2010)
* Fix for JENKINS-6655: JDK 1.5 compatibility. (Thanks to vlatombe)
* Bugfix in exception handling

## Version 1.4 (May 11 2010)
* Fix for JENKINS-5864: Plugin requires admin rights.
* Provide the following additional features.  The configurations are optional and can be set via the "Manage Hudson"->"Configure System" link. (Many thanks to John Borghi and his team for this enhancement)
* Alternative root folder for storing history. This option must be used if interested in preserving configurations for deleted jobs.
* Setting for the maximum number of history configurations to keep (per item).  Leave blank or zero to keep all entries (no maximum).
* Option to not create a new history entry if it is the same as the last saved entry for that item.
* Allow saving of 'system' configurations - defined as those stored directly in HUDSON_ROOT.
* Regexp pattern for excluding system configuration files to save. Useful because some system configurations are not interesting to save. A recommended default value is provided.
* Enhanced UI for selecting versions to compare.
* Allow system configuration diffs to be viewed via the UI.

## Version 1.3 (Apr 05 2010)
* Fix for JENKINS-6163: Job Config History badge is lost with newer versions of hudson.

## Version 1.2 (Feb 12 2010)
Replaced the homegrown version of diff with gnu unified diff.
* Fix for JENKINS-5534: Access permissions are not taken into account when getting files via jobConfigHistory.
* Fix for JENKINS-5607: table entry for username was empty.

## Version 1.1 (Feb 01 2010)
* First version of this plugin as described in JENKINS-2765.
