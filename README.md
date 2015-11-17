amls-frontend
=============

To update from Nexus and start all Services from the SNAPSHOT

 sm --start AMLS_ALL -f

To update from Nexus and start all Services from the RELEASE Version instead of SNAPSHOT

 sm --start AMLS_ALL -r

To stop the Services

 sm --stop AMLS_ALL

To start the app locally:

 sbt "run 9220"
