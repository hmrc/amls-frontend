/*
 * Copyright 2019 HM Revenue & Customs
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

import config.{AppConfig, WSHttp}
import javax.inject.Inject
import models.notifications.{NotificationDetails, NotificationRow}
import play.api.Logger
import play.api.libs.json.Writes
import uk.gov.hmrc.http._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AmlsNotificationConnector @Inject()(val http: WSHttp,
                                          val appConfig: AppConfig) {

  private[connectors] def baseUrl : String = appConfig.allNotificationsUrl

  def fetchAllByAmlsRegNo(amlsRegistrationNumber: String, accountTypeId: (String, String))
                         (implicit headerCarrier: HeaderCarrier, reqW: Writes[Seq[NotificationRow]]): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = s"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsNotificationConnector][fetchAllByAmlsRegNo]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    http.GET[Seq[NotificationRow]](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: $response")
        response
    }
  }

  def fetchAllBySafeId(safeId: String, accountTypeId: (String, String))
                      (implicit headerCarrier: HeaderCarrier, reqW: Writes[Seq[NotificationRow]]): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = accountTypeId

    val getUrl = s"$baseUrl/$accountType/$accountId/safeId/$safeId"
    val prefix = "[AmlsNotificationConnector][fetchAllBySafeId]"
    Logger.debug(s"$prefix - Request : $safeId")
    http.GET[Seq[NotificationRow]](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: $response")
        response
    }
  }

  def getMessageDetailsByAmlsRegNo(amlsRegistrationNumber: String, contactNumber: String, accountTypeId: (String, String))
                                  (implicit hc : HeaderCarrier, ec : ExecutionContext): Future[Option[NotificationDetails]]= {

    val (accountType, accountId) = accountTypeId

    val url = s"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/$contactNumber"
    http.GET[NotificationDetails](url)
      .map {Some(_)}
      .recover {
        case _:NotFoundException => None
      }
  }
}
