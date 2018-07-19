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

package services

import config.AppConfig
import javax.inject.Inject
import models.enrolment.{AmlsEnrolmentKey, TaxEnrolment}
import connectors.{AuthConnector, TaxEnrolmentsConnector, EnrolmentStubConnector}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class AuthEnrolmentsService @Inject()(val authConnector: AuthConnector,
                                      val enrolmentStore: TaxEnrolmentsConnector,
                                      val config: AppConfig,
                                      val stubConnector: EnrolmentStubConnector) {

  private val amlsKey = "HMRC-MLR-ORG"
  private val amlsNumberKey = "MLRRefNumber"

  def amlsRegistrationNumber(implicit authContext: AuthContext,
                             headerCarrier: HeaderCarrier,
                             ec: ExecutionContext): Future[Option[String]] = {

    val authEnrolments = authContext.enrolmentsUri map { uri =>
      authConnector.enrolments(uri)
    } getOrElse Future.successful(Seq.empty)

    lazy val stubbedEnrolments = if (config.enrolmentStubsEnabled) {
      authConnector.userDetails flatMap { details =>
        stubConnector.enrolments(details.groupIdentifier.getOrElse(throw new Exception("Group ID is unavailable")))
      }
    } else {
      Future.successful(Seq.empty)
    }

    val enrolmentQuery = authEnrolments flatMap {
      case enrolments if enrolments.count(_.key == amlsKey) > 0 => Future.successful(enrolments)
      case _ => stubbedEnrolments
    }

    enrolmentQuery map { enrolmentsList =>
      for {
        amlsEnrolment <- enrolmentsList.find(enrolment => enrolment.key == amlsKey)
        amlsIdentifier <- amlsEnrolment.identifiers.find(identifier => identifier.key == amlsNumberKey)
      } yield {
        val prefix = "[AuthEnrolmentsService][amlsRegistrationNumber]"
        Logger.debug(s"$prefix : ${amlsIdentifier.value}")
        amlsIdentifier.value
      }
    }
  }

  def enrol(amlsRegistrationNumber: String, postcode: String)
           (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[HttpResponse] = {
    authConnector.getCurrentAuthority flatMap {
      authority =>
        enrolmentStore.enrol(AmlsEnrolmentKey(amlsRegistrationNumber), TaxEnrolment(authority.credId, postcode))
    }
  }

  def deEnrol(amlsRegistrationNumber: String)
             (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext): Future[Boolean] = {
    for {
      _ <- enrolmentStore.removeKnownFacts(amlsRegistrationNumber)
      _ <- enrolmentStore.deEnrol(amlsRegistrationNumber)
    } yield true
  }

}
