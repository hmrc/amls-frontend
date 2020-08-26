/*
 * Copyright 2020 HM Revenue & Customs
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
import models.businessmatching.HighValueDealing
import models.hvd.{Alcohol, Hvd, Products, Tobacco}
import play.api.mvc.{Call, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DateOfChangeHelper
import views.html.hvd.products

import scala.concurrent.Future

class ProductsController @Inject() (val dataCacheConnector: DataCacheConnector,
                                    val statusService: StatusService,
                                    val authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val serviceFlow: ServiceFlow,
                                    val cc: MessagesControllerComponents,
                                    products: products) extends AmlsBaseController(ds, cc) with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
          response =>
            val form: Form2[Products] = (for {
              hvd <- response
              products <- hvd.products
            } yield Form2[Products](products)).getOrElse(EmptyForm)
            Ok(products(form, edit))
        }
  }


  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
          Form2[Products](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(products(f, edit)))
            case ValidForm(_, data) => {
              for {
                hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
                status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
                _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key, hvd.products(data))
                isNewActivity <- serviceFlow.isNewActivity(request.credId, HighValueDealing)
              } yield {
                val redirect = !isNewActivity && redirectToDateOfChange[Products](status, hvd.products, data)
                val exciseGoods = data.items.contains(Alcohol) | data.items.contains(Tobacco)
                Redirect(getNextPage(redirect, exciseGoods, edit))
              }
            }
          }
    }

  private def getNextPage(redirect: Boolean, exciseGoods: Boolean, edit:Boolean): Call = {
    (redirect, exciseGoods, edit) match {
      case (true, true, true)    => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoodsEdit)
      case (true, true, false)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.exciseGoods)
      case (true, false, true)   => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.checkYourAnswers)
      case (true, false, false)  => routes.HvdDateOfChangeController.get(DateOfChangeRedirect.howWillYouSellGoods)
      case (false, true, _)      => routes.ExciseGoodsController.get(edit)
      case (false, false, true)  => routes.SummaryController.get()
      case (false, false, false) => routes.HowWillYouSellGoodsController.get()
    }
  }
}