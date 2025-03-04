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

package controllers.bankdetails

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.bankdetails.BankDetails
import models.bankdetails.BankDetails.Filters._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.bankdetails.WhatYouNeedView

import javax.inject.Inject

class WhatYouNeedController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  dataCacheConnector: DataCacheConnector,
  val mcc: MessagesControllerComponents,
  view: WhatYouNeedView
) extends AmlsBaseController(ds, mcc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    val result = for {
      bankDetails <- OptionT(dataCacheConnector.fetch[Seq[BankDetails]](request.credId, BankDetails.key))
    } yield
      if (bankDetails.exists(visibleAccountsFilter)) {
        Ok(view(routes.BankAccountNameController.getNoIndex))
      } else {
        Ok(view(routes.HasBankAccountController.get))
      }

    result.map(_.removingFromSession("itemIndex")) getOrElse Ok(view(routes.HasBankAccountController.get))
      .removingFromSession("itemIndex")
  }
}
