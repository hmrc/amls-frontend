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

import javax.inject.Inject
import connectors.FeeConnector
import models.FeeResponse
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.status.RenewalSubmitted
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class FeeResponseService @Inject()(val feeConnector: FeeConnector) {

  def getFeeResponse(amlsReferenceNumber: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[FeeResponse]] = {

    feeConnector.feeResponse(amlsReferenceNumber) map ( feeResponse =>
      feeResponse.responseType match {
        case AmendOrVariationResponseType
          if feeResponse.difference.fold(false)(_ > 0) | feeResponse.totalFees > 0 => Some(feeResponse)
        case SubscriptionResponseType if feeResponse.totalFees > 0 => Some(feeResponse)
        case _ => None
    })
  } recoverWith {
    case _: NotFoundException => Future.successful(None)
  }

}
