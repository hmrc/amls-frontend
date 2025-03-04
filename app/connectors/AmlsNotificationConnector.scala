/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import config.ApplicationConfig

import javax.inject.Inject
import models.notifications.{NotificationDetails, NotificationRow}
import play.api.Logging
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

class AmlsNotificationConnector @Inject() (http: HttpClientV2, appConfig: ApplicationConfig)(implicit
  ec: ExecutionContext
) extends Logging {

  private[connectors] def baseUrl: String = appConfig.allNotificationsUrl

  def fetchAllByAmlsRegNo(amlsRegistrationNumber: String, accountTypeId: (String, String))(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsNotificationConnector][fetchAllByAmlsRegNo]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    // $COVERAGE-ON$
    http.get(getUrl).execute[Seq[NotificationRow]] map { response =>
      // $COVERAGE-OFF$
      logger.debug(s"$prefix - Response Body: $response")
      // $COVERAGE-ON$
      response
    }
  }

  def fetchAllBySafeId(safeId: String, accountTypeId: (String, String))(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = url"$baseUrl/$accountType/$accountId/safeId/$safeId"
    val prefix = "[AmlsNotificationConnector][fetchAllBySafeId]"
    // $COVERAGE-OFF$
    logger.debug(s"$prefix - Request : $safeId")
    // $COVERAGE-ON$
    http.get(getUrl).execute[Seq[NotificationRow]] map { response =>
      // $COVERAGE-OFF$
      logger.debug(s"$prefix - Response Body: $response")
      // $COVERAGE-ON$
      response
    }
  }

  def getMessageDetailsByAmlsRegNo(
    amlsRegistrationNumber: String,
    contactNumber: String,
    accountTypeId: (String, String)
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NotificationDetails]] = {

    val (accountType, accountId) = accountTypeId
    val url                      = url"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/$contactNumber"
    http.get(url).execute[Option[NotificationDetails]]
  }
}
