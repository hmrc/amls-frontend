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
import forms.msb.MoneySourcesFormProvider
import models.businessmatching.BusinessMatching
import models.moneyservicebusiness._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import services.msb.MoneySourcesService
import utils.AuthAction
import views.html.msb.MoneySourcesView

import javax.inject.Inject
import scala.concurrent.Future

class MoneySourcesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  service: MoneySourcesService,
  formProvider: MoneySourcesFormProvider,
  view: MoneySourcesView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb          <- response
        currencies   <- msb.whichCurrencies
        moneySources <- currencies.moneySources
      } yield moneySources).fold(formProvider())(formProvider().fill)
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
            msb        <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _          <- dataCacheConnector
                            .save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key, updateMoneySources(msb, data))
            updatedMsb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            bm         <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield service.redirectToNextPage(updatedMsb, bm, edit).getOrElse(NotFound(notFoundView))
      )
  }

  private def updateMoneySources(
    oldMsb: Option[MoneyServiceBusiness],
    moneySources: MoneySources
  ): Option[MoneyServiceBusiness] =
    oldMsb match {
      case Some(msb) =>
        msb.whichCurrencies match {
          case Some(w) => Some(msb.whichCurrencies(w.moneySources(moneySources)))
          case _       => None
        }
      case _         => None
    }
}
