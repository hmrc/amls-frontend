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

package utils

import cats.data.OptionT
import cats.implicits._
import services.{AuthEnrolmentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait AmlsRefNumberBroker {
  private[utils] val statusService: StatusService
  private[utils] val submissionResponseService: SubmissionResponseService
  private[utils] val authEnrolmentsService: AuthEnrolmentsService

  def get(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) = (for {
    status <- OptionT.liftF(statusService.getStatus)
    (_, _, _, Left(amlsRefNo)) <- OptionT(submissionResponseService.getSubmissionData(status))
  } yield amlsRefNo) orElse OptionT(authEnrolmentsService.amlsRegistrationNumber)
}

object AmlsRefNumberBroker extends AmlsRefNumberBroker {
  private[utils] lazy val statusService = StatusService
  private[utils] lazy val submissionResponseService = SubmissionResponseService
  private[utils] lazy val authEnrolmentsService = AuthEnrolmentsService
}
