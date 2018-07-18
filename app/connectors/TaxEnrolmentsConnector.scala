/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject
import audit.{ESDeEnrolEvent, ESEnrolEvent, ESEnrolFailureEvent, ESRemoveKnownFactsEvent}
import config.{AppConfig, WSHttp}
import exceptions.{DuplicateEnrolmentException, InvalidEnrolmentCredentialsException}
import models.enrolment.ErrorResponse._
import models.enrolment.{AmlsEnrolmentKey, EnrolmentKey, EnrolmentStoreEnrolment, ErrorResponse}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}

class TaxEnrolmentsConnector @Inject()(http: WSHttp, appConfig: AppConfig, auth: AuthConnector, audit: AuditConnector) {

  lazy val baseUrl = s"${appConfig.enrolmentStoreUrl}/tax-enrolments"
  val warn: String => Unit = msg => Logger.warn(s"[TaxEnrolmentsConnector] $msg")

  object ResponseCodes {
    val duplicateEnrolment = "ERROR_INVALID_IDENTIFIERS"
    val invalidCredentialRole = "INVALID_CREDENTIAL_ID"
  }

  def enrol(enrolKey: EnrolmentKey, enrolment: EnrolmentStoreEnrolment)
           (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[HttpResponse] = {

    auth.userDetails flatMap { details =>
      details.groupIdentifier match {
        case Some(groupId) =>
          val url = s"$baseUrl/groups/$groupId/enrolments/${enrolKey.key}"

          http.POST[EnrolmentStoreEnrolment, HttpResponse](url, enrolment) map { response =>
            audit.sendEvent(ESEnrolEvent(enrolment, response, enrolKey))
            response
          } recoverWith {
            case e: Upstream4xxResponse if Json.parse(e.message).asOpt[ErrorResponse].isDefined =>
              val error = Json.parse(e.message).as[ErrorResponse]
              audit.sendEvent(ESEnrolFailureEvent(enrolment, e, enrolKey))
              warn(error.toString)

              (e.upstreamResponseCode, error.code) match {
                case (BAD_REQUEST, ResponseCodes.duplicateEnrolment) =>
                  throw DuplicateEnrolmentException(error.toString, e)
                case (FORBIDDEN, ResponseCodes.invalidCredentialRole) =>
                  throw InvalidEnrolmentCredentialsException(error.toString, e)
              }

            case e: Throwable =>
              audit.sendEvent(ESEnrolFailureEvent(enrolment, e, enrolKey))
              warn(e.getMessage)
              throw e
          }

        case _ => throw new Exception("Group identifier is unavailable")
      }
    }
  }

  def deEnrol(registrationNumber: String)
             (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[HttpResponse] = {
    val enrolKey = AmlsEnrolmentKey(registrationNumber).key

    auth.userDetails flatMap { details =>
      details.groupIdentifier match {
        case Some(groupId) =>
          val url = s"$baseUrl/groups/$groupId/enrolments/$enrolKey"

          http.DELETE(url) map { response =>
            audit.sendEvent(ESDeEnrolEvent(response, enrolKey))
            response
          }

        case _ => throw new Exception("Group identifier is unavailable")
      }
    }
  }

  def removeKnownFacts(registrationNumber: String)
                      (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[HttpResponse] = {

    val enrolKey = AmlsEnrolmentKey(registrationNumber).key
    val url = s"$baseUrl/enrolments/$enrolKey"

    http.DELETE(url) map { response =>
      audit.sendEvent(ESRemoveKnownFactsEvent(response, enrolKey))
      response
    }
  }

}
