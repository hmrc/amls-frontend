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
import forms.hvd.SalesChannelFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.{HowWillYouSellGoods, Hvd}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.{AuthAction, DateOfChangeHelper}
import views.html.hvd.HowWillYouSellGoodsView

import javax.inject.Inject
import scala.concurrent.Future

class HowWillYouSellGoodsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: SalesChannelFormProvider,
  view: HowWillYouSellGoodsView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map { response =>
      val form = (for {
        hvd      <- response
        channels <- hvd.howWillYouSellGoods
      } yield formProvider().fill(channels)).getOrElse(formProvider())
      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        model =>
          for {
            hvd           <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
            status        <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            _             <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.howWillYouSellGoods(model))
            isNewActivity <- serviceFlow.isNewActivity(request.credId, HighValueDealing)
          } yield {
            val redirect =
              !isNewActivity && redirectToDateOfChange[HowWillYouSellGoods](status, hvd.howWillYouSellGoods, model)
            Redirect(getNextPage(redirect, edit))
          }
      )
  }

  private def getNextPage(redirect: Boolean, edit: Boolean): Call =
    (redirect, edit) match {
      case (true, true)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false)  => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.cashPayment)
      case (false, true)  => routes.SummaryController.get
      case (false, false) => routes.CashPaymentController.get()
    }
}
