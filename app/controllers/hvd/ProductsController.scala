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
import forms.hvd.ProductsFormProvider
import models.businessmatching.BusinessActivity.HighValueDealing
import models.hvd.Products.{Alcohol, Tobacco}
import models.hvd.{Hvd, Products}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.{AuthAction, DateOfChangeHelper}
import views.html.hvd.ProductsView

import javax.inject.Inject
import scala.concurrent.Future

class ProductsController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: ProductsFormProvider,
  view: ProductsView
) extends AmlsBaseController(ds, cc)
    with DateOfChangeHelper {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map { response =>
      val form = (for {
        hvd      <- response
        products <- hvd.products
      } yield formProvider().fill(products)).getOrElse(formProvider())
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
            _             <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.products(data))
            isNewActivity <- serviceFlow.isNewActivity(request.credId, HighValueDealing)
          } yield {
            val redirect    = !isNewActivity && redirectToDateOfChange[Products](status, hvd.products, data)
            val exciseGoods = data.items.contains(Alcohol) | data.items.contains(Tobacco)
            Redirect(getNextPage(redirect, exciseGoods, edit))
          }
      )
  }

  private def getNextPage(redirect: Boolean, exciseGoods: Boolean, edit: Boolean): Call =
    (redirect, exciseGoods, edit) match {
      case (true, true, true)    => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoodsEdit)
      case (true, true, false)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoods)
      case (true, false, true)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false, false)  => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods)
      case (false, true, _)      => routes.ExciseGoodsController.get(edit)
      case (false, false, true)  => routes.SummaryController.get
      case (false, false, false) => routes.HowWillYouSellGoodsController.get()
    }
}
