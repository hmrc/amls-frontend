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

package services

import config.ApplicationConfig
import connectors.{EnrolmentStubConnector, TaxEnrolmentsConnector}
import javax.inject.Inject
import models.enrolment.{AmlsEnrolmentKey, TaxEnrolment}
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AuthEnrolmentsService @Inject() (
  val enrolmentStore: TaxEnrolmentsConnector,
  val config: ApplicationConfig,
  val stubConnector: EnrolmentStubConnector
) extends Logging {

  private val amlsKey       = "HMRC-MLR-ORG"
  private val amlsNumberKey = "MLRRefNumber"
  private val prefix        = "AuthEnrolmentsService"

  def amlsRegistrationNumber(amlsRegistrationNumber: Option[String], groupIdentifier: Option[String])(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[String]] = {

    // $COVERAGE-OFF$
    logger.debug(s"[$prefix][amlsRegistrationNumber] - Begin...)")
    logger.debug(s"[$prefix][amlsRegistrationNumber] - config.enrolmentStubsEnabled: ${config.enrolmentStubsEnabled})")
    // $COVERAGE-ON$

    val stubbedEnrolments = if (config.enrolmentStubsEnabled) {
      stubConnector.enrolments(groupIdentifier.getOrElse(throw new Exception("Group ID is unavailable")))
    } else {
      // $COVERAGE-OFF$
      logger.debug(s"[$prefix][amlsRegistrationNumber] - Returning empty sequence...)")
      // $COVERAGE-ON$
      Future.successful(Seq.empty)
    }

    amlsRegistrationNumber match {
      case regNo @ Some(_) => Future.successful(regNo)
      case None            =>
        stubbedEnrolments map { enrolmentsList =>
          // $COVERAGE-OFF$
          logger.debug(s"[$prefix][amlsRegistrationNumber] - enrolmentsList: $enrolmentsList)")
          // $COVERAGE-ON$
          for {
            amlsEnrolment  <- enrolmentsList.find(enrolment => enrolment.key == amlsKey)
            amlsIdentifier <- amlsEnrolment.identifiers.find(identifier => identifier.key == amlsNumberKey)
          } yield {
            // $COVERAGE-OFF$
            logger.debug(s"[$prefix][amlsRegistrationNumber] - amlsEnrolment: $amlsEnrolment)")
            logger.debug(s"[$prefix][amlsRegistrationNumber] : ${amlsIdentifier.value}")
            // $COVERAGE-ON$
            amlsIdentifier.value
          }
        }
    }
  }

  def enrol(amlsRegistrationNumber: String, postcode: String, groupId: Option[String], credId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[HttpResponse] =
    enrolmentStore.enrol(AmlsEnrolmentKey(amlsRegistrationNumber), TaxEnrolment(credId, postcode), groupId)

  def deEnrol(amlsRegistrationNumber: String, groupId: Option[String])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Boolean] =
    for {
      _ <- enrolmentStore.removeKnownFacts(amlsRegistrationNumber)
      _ <- enrolmentStore.deEnrol(amlsRegistrationNumber, groupId)
    } yield true

}
