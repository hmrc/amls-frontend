/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Inject

import connectors.AuthConnector
import models.enrolment.Constants
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class LegacyAuthEnrolmentService @Inject()(val authConnector: AuthConnector) {

  def amlsRegistrationNumber(implicit authContext: AuthContext,
                             headerCarrier: HeaderCarrier,
                             ec: ExecutionContext): Future[Option[String]] = {

    authContext.enrolmentsUri match {
      case Some(uri) =>

        val enrolments = authConnector.enrollments(uri)

        enrolments map {
          enrolmentsList => {
            for {
              amlsEnrolment <- enrolmentsList.find(enrolment => enrolment.key == Constants.serviceName)
              amlsIdentifier <- amlsEnrolment.identifiers.find(identifier => identifier.key == Constants.amlsRefIdentKey)
            } yield {
              val prefix = "[LegacyAuthEnrolmentsService][amlsRegistrationNumber]"
              Logger.debug(s"$prefix : ${amlsIdentifier.value}")
              amlsIdentifier.value
            }
          }
        }
      case None => Future.successful(None)
    }
  }
}