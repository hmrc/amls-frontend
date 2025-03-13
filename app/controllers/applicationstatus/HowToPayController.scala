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

package controllers.applicationstatus

import controllers.{AmlsBaseController, CommonPlayDependencies}

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, FeeHelper}
import views.html.applicationstatus.HowToPayView

@Singleton
class HowToPayController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  val feeHelper: FeeHelper,
  view: HowToPayView
) extends AmlsBaseController(ds, cc) {

  val prefix = "[HowToPayController]"

  def get: Action[AnyContent] = authAction.async { implicit request =>
    feeHelper.retrieveFeeResponse(request.amlsRefNumber, request.accountTypeId, request.groupIdentifier, prefix) map {
      case Some(fees) if !isEmpty(fees.paymentReference) => Ok(view(fees.paymentReference))
      case _                                             => Ok(view(None))
    }
  }

  def isEmpty(x: Option[String]): Boolean =
    x.isEmpty || x == null || (x.isDefined && x.get.trim.isEmpty)
}
