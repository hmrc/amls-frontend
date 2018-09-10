AMLS Notification Package Template Versioning
=============

**Business Background**

* AMLS notification template packages are version controlled, so that a change to the wording in the template used, is only reflected in future messages, and other messages continue to use their original version of the template.

* It will potentially create legal issues if the wording shown on an occasion in the past differs from what is displayed currently.

**Technical Background**

* Whenever a AMLS notification is sent to the customer, a message is sent from ETMP through to amls-notification which stores the summary information, along with the current package version number which is currently active in its Mongo database

* All of the templates are packaged together under a version number within amls-frontend, with an associated script to handle the upversioning (detailed below).

* In order to ensure that the templates for a previous version are not changed by accident, each one is tagged with a magic number calculated based on its contents. Should this number change for a previous version then the unit tests will automatically fail.

* The version numbers are in the format v{`MAJOR`}m{`MINOR`}, starting from v1m0.


Creating a new package template version
=============

**To create a new Major version :-**

* Run the script below from the amls-frontend directory, this will create a new major package, copy the existing templates and unit tests into it.
```
$ upversion-templates.sh
```

* Make the change to the template you want to change
* Run the unit test using `testOnly NotificationsCheckSumSpec` and look for the line `Replace checksum for ${versionNumber}/${fileName} with ${ checkSum }`
* update the magic number in the relevant file in ./conf/notifications/`{ versionNumber }` with the checksum and rerun the test



**To create a new Minor version :-**

* Run the script below from the amls-frontend directory, this will create a new minor package, copy the existing templates and unit tests into it.
```
$ upversion-templates.sh minor
```

* Make the change to the template you want to change
* Run the unit test using `testOnly NotificationsCheckSumSpec` and look for the line `Replace checksum for ${versionNumber}/${fileName} with ${ checkSum }`
* update the magic number in the relevant file in ./conf/notifications/`{ versionNumber }` with the checksum and rerun the test


Deployment considerations
=============

* **Its important that amls-frontend is deployed prior to amls-notification (to ensure the newly versioned templates are available)**
* Once you ready to deploy then change the `microservices.services.current-template-package-version` in the correct `app-config` (e.g. `app-config-qa`) and then re-deploy the last package
 