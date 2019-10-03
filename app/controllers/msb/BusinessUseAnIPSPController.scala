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

package controllers.msb


import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.moneyservicebusiness._
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.msb.business_use_an_ipsp

import scala.concurrent.Future

class BusinessUseAnIPSPController @Inject() (val dataCacheConnector: DataCacheConnector,
                                             authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[BusinessUseAnIPSP] = (for {
            msb <- response
            businessUseAnIPSP <- msb.businessUseAnIPSP
          } yield Form2[BusinessUseAnIPSP](businessUseAnIPSP)).getOrElse(EmptyForm)
          Ok(business_use_an_ipsp(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[BusinessUseAnIPSP](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(business_use_an_ipsp(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
              msb.businessUseAnIPSP(data))

          } yield edit match {
            case true if msb.fundsTransfer.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ =>
              Redirect(routes.FundsTransferController.get(edit))
          }
      }
    }
  }
}
