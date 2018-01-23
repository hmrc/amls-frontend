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
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class FeeResponseService @Inject()(
                                  val feeConnector: FeeConnector
                                  ){

  def getFeeResponse(amlsReferenceNumber: String)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[FeeResponse]] = {
    feeConnector.feeResponse(amlsReferenceNumber).map(x => x.responseType match {
      case AmendOrVariationResponseType if x.difference.fold(false)(_ > 0) => Some(x)
      case SubscriptionResponseType if x.totalFees > 0 => Some(x)
      case _ => None
    })
  }.recoverWith {
    case _: NotFoundException => Future.successful(None)
  }

}
