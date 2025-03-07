amls-frontend
=============

This is the repository for the Anti-Money Laundering Supervision frontend. This service provides users the ability to register for supervision
and update or vary their supervision details.

## Running the service

### Using service manager

Using [sm2](https://github.com/hmrc/sm2)
with the service manager profile `AMLS_ALL` will start
all the Anti Money Laundering Service microservices as well as the services
that they depend on.

```
sm2 --start AMLS_ALL
```

To stop the frontend microservice from running on service manager (e.g. to run your own version locally), you can run:

```
sm2 -stop AMLS_FRONTEND
```

### Using localhost

To run this frontend microservice locally on the configured port **'9222'**, you can run:

```
sbt run 
```

**NOTE:** Ensure that you are not running the microservice via service manager before starting your service locally (vice versa) or the service will fail to start

To run the service with test-only routes enabled, run the script `run-local-no-inappconf.sh`.


## Accessing the service

This service requires authentication stubbing before it can be accessed. Details can be found on the
[DDCY Live Services Credentials sheet](https://docs.google.com/spreadsheets/d/1ecLTROmzZtv97jxM-5LgoujinGxmDoAuZauu2tFoAVU/edit?gid=1186990023#gid=1186990023)
for both staging and local url's or check the Tech Overview section in the
[service runbook ](https://confluence.tools.tax.service.gov.uk/display/ELSY/AMLS+Service+Summary)

Additional test data can be found within the [amls-stub](https://github.com/hmrc/amls-stub) repositories. Check the README to see different enrollment values and data states.


## Running tests via terminal

To run tests in Intellij, you should increase the heapstack by running:

```
sbt test -mem 2048
```

## Other helpful documentation

* [Service Runbook](https://confluence.tools.tax.service.gov.uk/display/ELSY/Anti+Money+Laundering+Supervision+%28AMLS%29+Runbook)

* [Architecture Links](https://confluence.tools.tax.service.gov.uk/display/ELSY/AMLS+Architecture)

* [Creating new notification template versions](documentation/template-versions.md)
 



