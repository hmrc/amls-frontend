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
import forms.msb.SendMoneyToOtherCountryFormProvider
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, ForeignExchange}
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService}
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.AuthAction
import views.html.msb.SendMoneyToOtherCountryView

import javax.inject.Inject
import scala.concurrent.Future

class SendMoneyToOtherCountryController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: SendMoneyToOtherCountryFormProvider,
  view: SendMoneyToOtherCountryView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map { response =>
      val form = (for {
        msb   <- response
        money <- msb.sendMoneyToOtherCountry
      } yield formProvider().fill(money)).getOrElse(formProvider())

      Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
            val result = for {
              cache    <- maybeCache
              msb      <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
              bm       <- cache.getEntry[BusinessMatching](BusinessMatching.key)
              services <- bm.msbServices
              register <-
                cache.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
            } yield
              if (data.money) {
                dataCacheConnector
                  .save(request.credId, MoneyServiceBusiness.key, msb.sendMoneyToOtherCountry(data)) map { _ =>
                  Redirect(routes.SendTheLargestAmountsOfMoneyController.get(edit))
                }
              } else {
                val newModel = msb
                  .sendMoneyToOtherCountry(data)
                  .sendTheLargestAmountsOfMoney(None)
                  .mostTransactions(None)

                dataCacheConnector.save(request.credId, MoneyServiceBusiness.key, newModel) map { _ =>
                  if (edit) {
                    Redirect(routes.SummaryController.get)
                  } else {
                    routing(services.msbServices, register, newModel, edit)
                  }
                }
              }
            result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
          }
      )
  }

  private def shouldAnswerCurrencyExchangeQuestion(
    services: Set[BusinessMatchingMsbService],
    register: ServiceChangeRegister,
    msb: MoneyServiceBusiness
  ): Boolean =
    currencyExchangeAddedPostSubmission(services, register) ||
      (services.contains(CurrencyExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)

  private def shouldAnswerForeignExchangeQuestion(
    services: Set[BusinessMatchingMsbService],
    register: ServiceChangeRegister,
    msb: MoneyServiceBusiness
  ): Boolean =
    foreignExchangeAddedPostSubmission(services, register) ||
      (services.contains(ForeignExchange) && msb.sendTheLargestAmountsOfMoney.isEmpty)

  private def routing(
    services: Set[BusinessMatchingMsbService],
    register: ServiceChangeRegister,
    msb: MoneyServiceBusiness,
    edit: Boolean
  ) = {

    val (ceQuestion, fxQuestion) = (
      shouldAnswerCurrencyExchangeQuestion(services, register, msb),
      shouldAnswerForeignExchangeQuestion(services, register, msb)
    )

    (ceQuestion, fxQuestion) match {
      case (true, _) => Redirect(routes.CurrencyExchangesInNext12MonthsController.get(edit))
      case (_, true) => Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
      case _         => Redirect(routes.SummaryController.get)
    }
  }
}
