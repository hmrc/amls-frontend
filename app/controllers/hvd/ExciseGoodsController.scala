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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.hvd.ExciseGoodsFormProvider

import javax.inject.Inject
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.{ExciseGoods, Hvd}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import utils.DateOfChangeHelper
import views.html.hvd.ExciseGoodsView

import scala.concurrent.Future

class ExciseGoodsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: ExciseGoodsFormProvider,
  view: ExciseGoodsView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map { response =>
      val form = (for {
        hvd         <- response
        exciseGoods <- hvd.exciseGoods
      } yield formProvider().fill(exciseGoods)).getOrElse(formProvider())
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
            hvd           <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
            status        <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            _             <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.exciseGoods(data))
            isNewActivity <- serviceFlow.isNewActivity(request.credId, HighValueDealing)
          } yield {
            val redirect = !isNewActivity && redirectToDateOfChange[ExciseGoods](status, hvd.exciseGoods, data)
            Redirect(getNextPage(redirect, edit))
          }
      )
  }

  private def getNextPage(redirect: Boolean, edit: Boolean): Call =
    (redirect, edit) match {
      case (true, true)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false)  => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods)
      case (false, true)  => routes.SummaryController.get
      case (false, false) => routes.HowWillYouSellGoodsController.get()
    }
}
