amls-frontend
=============

To update from Nexus and start all Services from the SNAPSHOT

 sm --start AMLS_ALL -f

To update from Nexus and start all Services from the RELEASE Version instead of SNAPSHOT

 sm --start AMLS_ALL -r

To stop the Services

 sm --stop AMLS_ALL

To start the app locally, kill the service AMLS_FRONTEND and then start using the command below

 sbt "run 9222"

To update Service Manager, do as below

 sudo pip install servicemanager --upgrade
