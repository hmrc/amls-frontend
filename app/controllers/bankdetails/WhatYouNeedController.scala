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

package controllers.bankdetails

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.bankdetails.BankDetails
import models.bankdetails.BankDetails.Filters._
import play.api.mvc.{Call, MessagesControllerComponents}
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.bankdetails._

class WhatYouNeedController @Inject()(val authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      dataCacheConnector: DataCacheConnector,
                                      val mcc: MessagesControllerComponents) extends AmlsBaseController(ds, mcc) {

  def get = authAction.async {
      implicit request =>
        val view = what_you_need.apply(_: Call)

        val result = for {
            bankDetails <- OptionT(dataCacheConnector.fetch[Seq[BankDetails]](request.credId, BankDetails.key))
          } yield {
            if (bankDetails.exists(visibleAccountsFilter)) {
              Ok(view(routes.BankAccountNameController.getNoIndex()))
            } else {
              Ok(view(routes.HasBankAccountController.get()))
            }
          }

        result getOrElse Ok(view(routes.HasBankAccountController.get()))
  }
}
