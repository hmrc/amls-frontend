/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, FeeHelper}
import views.html.applicationstatus.how_to_pay

@Singleton
class HowToPayController @Inject()(authAction: AuthAction,
                                   val ds: CommonPlayDependencies,
                                   val cc: MessagesControllerComponents,
                                   val feeHelper: FeeHelper,
                                   how_to_pay: how_to_pay) extends AmlsBaseController(ds, cc) {

  val prefix = "[HowToPayController]"

  def get = authAction.async {
    implicit request =>
      feeHelper.retrieveFeeResponse(request.amlsRefNumber, request.accountTypeId, request.groupIdentifier, prefix) map {
        case Some(fees) if !isEmpty(fees.paymentReference) => Ok(how_to_pay(fees.paymentReference))
        case _          => Ok(how_to_pay(None))
      }
  }

  def isEmpty(x: Option[String]) = {
    x.isEmpty || x == null || (x.isDefined && x.get.trim.isEmpty)
  }
}