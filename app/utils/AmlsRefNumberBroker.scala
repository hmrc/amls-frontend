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

package utils

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import models.confirmation.SubmissionData
import play.api.Play
import services.{AuthEnrolmentsService, StatusService, SubmissionResponseService}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class AmlsRefNumberBroker @Inject()(
                                     private[utils] val statusService: StatusService,
                                     private[utils] val authEnrolmentsService: AuthEnrolmentsService
                                   ) {

  private[utils] val submissionResponseService: SubmissionResponseService = Play.current.injector.instanceOf[SubmissionResponseService]

  def get(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) = (for {
    status <- OptionT.liftF(statusService.getStatus)
    SubmissionData(_, _, _, Some(amlsRefNo), None) <- OptionT(submissionResponseService.getSubmissionData(status, None))
  } yield amlsRefNo) orElse OptionT(authEnrolmentsService.amlsRegistrationNumber)
}