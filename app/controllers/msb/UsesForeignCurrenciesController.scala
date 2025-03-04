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
import forms.msb.UsesForeignCurrenciesFormProvider

import javax.inject.Inject
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService}
import models.businessmatching.BusinessMatchingMsbService.ForeignExchange
import models.moneyservicebusiness._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import views.html.msb.UsesForeignCurrenciesView

import scala.concurrent.Future

class UsesForeignCurrenciesController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val dataCacheConnector: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow,
  val cc: MessagesControllerComponents,
  formProvider: UsesForeignCurrenciesFormProvider,
  view: UsesForeignCurrenciesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb         <- response
        currencies  <- msb.whichCurrencies
        usesForeign <- currencies.usesForeignCurrencies
      } yield usesForeign).fold(formProvider())(formProvider().fill)

      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false) = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
            val result = for {
              cacheMap <- maybeCache
              msb      <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
              bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              services <- bm.msbServices
              register <-
                cacheMap.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
            } yield dataCacheConnector
              .save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key, updateCurrencies(msb, data)) map {
              _ => routing(services.msbServices, register, msb, edit, data)
            }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  def updateCurrencies(
    oldMsb: MoneyServiceBusiness,
    usesForeignCurrencies: UsesForeignCurrencies
  ): Option[MoneyServiceBusiness] =
    (oldMsb.whichCurrencies, usesForeignCurrencies) match {
      case (Some(w), UsesForeignCurrenciesYes) =>
        Some(oldMsb.whichCurrencies(w.usesForeignCurrencies(usesForeignCurrencies)))
      case (Some(w), UsesForeignCurrenciesNo)  =>
        Some(oldMsb.whichCurrencies(w.usesForeignCurrencies(usesForeignCurrencies).moneySources(MoneySources())))
      case (_, _)                              => None
    }

  private def shouldAnswerForeignExchangeQuestions(
    msbServices: Set[BusinessMatchingMsbService],
    register: ServiceChangeRegister,
    msb: MoneyServiceBusiness,
    edit: Boolean
  ): Boolean =
    foreignExchangeAddedPostSubmission(msbServices, register) ||
      (msbServices.contains(ForeignExchange) && (msb.fxTransactionsInNext12Months.isEmpty || !edit))

  private def routing(
    msbServices: Set[BusinessMatchingMsbService],
    register: ServiceChangeRegister,
    msb: MoneyServiceBusiness,
    edit: Boolean,
    data: UsesForeignCurrencies
  ) =
    if (data == UsesForeignCurrenciesYes) {
      Redirect(routes.MoneySourcesController.get(edit))
    } else if (shouldAnswerForeignExchangeQuestions(msbServices, register, msb, edit)) {
      Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
    } else {
      Redirect(routes.SummaryController.get)
    }
}
