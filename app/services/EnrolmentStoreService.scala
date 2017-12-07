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

import connectors.EnrolmentStoreConnector
import models.enrolment.{EnrolmentEntry, EnrolmentIdentifier}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreService @Inject()(connector: EnrolmentStoreConnector) {

  def getAmlsRegistrationNumber(implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] =
    connector.userEnrolments(authContext.user.userId) map {
      case Some(enrolment) if enrolment.totalRecords > 0 && enrolment.enrolments.nonEmpty =>
        enrolment.enrolments.collectFirst {
          case EnrolmentEntry("HMRC-MLR-ORG", _, _, idents) if idents.nonEmpty =>
            idents.collectFirst {
              case EnrolmentIdentifier("MLRRefNumber", refNumber) => refNumber
            }
        }.flatten
      case _ => None
    }

}
