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

package controllers.msb

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.msb.BusinessUseAnIPSPFormProvider
import models.moneyservicebusiness._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.msb.BusinessUseAnIPSPView

import javax.inject.Inject
import scala.concurrent.Future

class BusinessUseAnIPSPController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: BusinessUseAnIPSPFormProvider,
  view: BusinessUseAnIPSPView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb               <- response
        businessUseAnIPSP <- msb.businessUseAnIPSP
      } yield formProvider().fill(businessUseAnIPSP)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _   <- dataCacheConnector
                     .save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key, msb.businessUseAnIPSP(data))

          } yield edit match {
            case true if msb.fundsTransfer.isDefined =>
              Redirect(routes.SummaryController.get)
            case _                                   =>
              Redirect(routes.FundsTransferController.get(edit))
          }
      )
  }
}
