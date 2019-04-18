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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.HighValueDealing
import models.hvd.{ExciseGoods, Hvd}
import play.api.mvc.Call
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DateOfChangeHelper
import views.html.hvd.excise_goods

import scala.concurrent.Future

class ExciseGoodsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                       val statusService: StatusService,
                                       val authConnector: AuthConnector,
                                       val serviceFlow: ServiceFlow
                                      ) extends BaseController with DateOfChangeHelper {

  def get(edit: Boolean = false) =
    Authorised.async {
      implicit authContext =>
        implicit request =>
          dataCacheConnector.fetch[Hvd](Hvd.key) map {
            response =>
              val form: Form2[ExciseGoods] = (for {
                hvd <- response
                exciseGoods <- hvd.exciseGoods
              } yield Form2[ExciseGoods](exciseGoods)).getOrElse(EmptyForm)
              Ok(excise_goods(form, edit))
          }
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[ExciseGoods](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(excise_goods(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              status <- statusService.getStatus
              _ <- dataCacheConnector.save[Hvd](Hvd.key, hvd.exciseGoods(data))
              isNewActivity <- serviceFlow.isNewActivity(HighValueDealing)
            } yield {
              val redirect = !isNewActivity && redirectToDateOfChange[ExciseGoods](status, hvd.exciseGoods, data)
              Redirect(getNextPage(redirect, edit))
            }
        }
      }
  }

  private def getNextPage(redirect: Boolean, edit:Boolean): Call = {
    (redirect,  edit) match {
      case (true, true) =>  routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false) => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods)
      case (false, true) => routes.SummaryController.get()
      case (false, false) => routes.HowWillYouSellGoodsController.get()
    }
  }
}
