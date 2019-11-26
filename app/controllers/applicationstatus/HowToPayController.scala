/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.applicationstatus

import controllers.DefaultBaseController
import javax.inject.{Inject, Singleton}
import utils.{AuthAction, FeeHelper}
import views.html.applicationstatus.how_to_pay

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class HowToPayController @Inject()(authAction: AuthAction,
                                   val feeHelper: FeeHelper) extends DefaultBaseController {

  val prefix = "[HowToPayController]"

  def get = authAction.async {
    implicit request =>
      feeHelper.retrieveFeeResponse(request.amlsRefNumber, request.accountTypeId, request.groupIdentifier, prefix) map {
        case Some(fees) => Ok(how_to_pay(fees.paymentReference))
        case _          => Ok(how_to_pay(None))
      }
  }
}