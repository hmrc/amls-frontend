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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.{HowWillYouSellGoods, Hvd}
import play.api.mvc.{Call, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction

import utils.DateOfChangeHelper
import views.html.hvd.how_will_you_sell_goods

import scala.concurrent.Future

class HowWillYouSellGoodsController @Inject()( val dataCacheConnector: DataCacheConnector,
                                               val statusService: StatusService,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val serviceFlow: ServiceFlow,
                                               val cc: MessagesControllerComponents,
                                               how_will_you_sell_goods: how_will_you_sell_goods) extends AmlsBaseController(ds, cc) with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[HowWillYouSellGoods] = (for {
              hvd <- response
              channels <- hvd.howWillYouSellGoods
            } yield Form2[HowWillYouSellGoods](channels)).getOrElse(EmptyForm)
            Ok(how_will_you_sell_goods(form, edit))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request => {
        Form2[HowWillYouSellGoods](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(how_will_you_sell_goods(f, edit)))
          case ValidForm(_, model) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
              status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.howWillYouSellGoods(model))
              isNewActivity <- serviceFlow.isNewActivity(request.credId, HighValueDealing)
            } yield {
              val redirect = !isNewActivity && redirectToDateOfChange[HowWillYouSellGoods](status, hvd.howWillYouSellGoods, model)
              Redirect(getNextPage(redirect, edit))
            }
        }
      }
  }

  private def getNextPage(redirect: Boolean, edit:Boolean): Call = {
    (redirect,  edit) match {
      case (true, true)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false)  => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.cashPayment)
      case (false, true)  => routes.SummaryController.get
      case (false, false) => routes.CashPaymentController.get()
    }
  }
}
