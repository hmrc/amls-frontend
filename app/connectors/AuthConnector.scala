/*
 * Copyright 2020 HM Revenue & Customs
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
import models.enrolment.GovernmentGatewayEnrolment
import org.apache.http.HttpStatus
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.controllers.RestFormats
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

import scala.concurrent.{ExecutionContext, Future}

case class Ids(internalId: String)

object Ids {
  implicit val format = Json.format[Ids]
}

case class Authority(uri: String,
                     accounts: Accounts,
                     userDetailsLink: String,
                     ids: String,
                     credId: String
                    ) {

  def normalisedIds: String = if (ids.startsWith("/")) ids.drop(1) else ids
}

// $COVERAGE-OFF$
object Authority {
  implicit val format = {
    implicit val dateFormat = RestFormats.dateTimeFormats
    implicit val accountsFormat = Accounts.format
    Json.format[Authority]
  }
}
// $COVERAGE-ON$

class AuthConnector @Inject()(val http: WSHttp, config: AppConfig) {

  private lazy val authUrl = config.authUrl

  def enrolments(uri: String)(implicit
                              headerCarrier: HeaderCarrier,
                              ec: ExecutionContext): Future[List[GovernmentGatewayEnrolment]] = {

    http.GET[List[GovernmentGatewayEnrolment]](authUrl + uri)
  }

  def getCurrentAuthority(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Authority] = {
    http.GET[Authority](s"$authUrl/auth/authority").recoverWith {
      case (t: Upstream4xxResponse) if t.upstreamResponseCode == HttpStatus.SC_UNAUTHORIZED => Future.failed(new Exception("Bearer token expired"))

    }
  }

  def getIds(authority: Authority)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Ids] = {
    http.GET[Ids](s"$authUrl/${authority.normalisedIds}")
  }

  def getCredId(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext) = getCurrentAuthority flatMap {
    case authority => Future.successful(authority.credId)
    case _ => Future.failed(new NotFoundException("No credId available"))
  }
}

