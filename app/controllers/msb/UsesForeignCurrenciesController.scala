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

package controllers.msb

import connectors.DataCacheConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.updateservice.ServiceChangeRegister
import models.businessmatching.{BusinessMatching, BusinessMatchingMsbService, CurrencyExchange, ForeignExchange, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness._
import play.api.mvc.Result
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.ControllerHelper

import scala.concurrent.Future

class UsesForeignCurrenciesController @Inject()(val authConnector: AuthConnector,
                                                implicit val dataCacheConnector: DataCacheConnector,
                                                implicit val statusService: StatusService,
                                                implicit val serviceFlow: ServiceFlow
                                               ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        ControllerHelper.allowedToEdit(MsbActivity, Some(CurrencyExchange)) flatMap {
          case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
            response =>
              val form: Form2[UsesForeignCurrencies] = (for {
                msb <- response
                currencies <- msb.whichCurrencies
                usesForeign <- currencies.usesForeignCurrencies
              } yield Form2[UsesForeignCurrencies](usesForeign)).getOrElse(EmptyForm)

              Ok(views.html.msb.uses_foreign_currencies(form, edit))
          }
          case false => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        val foo = Form2[UsesForeignCurrencies](request.body)
        foo match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.msb.uses_foreign_currencies(f, edit)))
          case ValidForm(_, data: UsesForeignCurrencies) =>
            dataCacheConnector.fetchAll flatMap { maybeCache =>
              val result = for {
                cacheMap <- maybeCache
                msb <- cacheMap.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
                bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                services <- bm.msbServices
                register <- cacheMap.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
              } yield {
                dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
                  updateCurrencies(msb, data)) flatMap {
                  _ => routing(services.msbServices, register, msb, edit, data)
                }
              }

              result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
      }
  }

  def updateCurrencies(oldMsb: MoneyServiceBusiness, usesForeignCurrencies: UsesForeignCurrencies): Option[MoneyServiceBusiness] = {
    oldMsb match {
      case msb => {
        msb.whichCurrencies match {
          case Some(w) => {
            Some(msb.whichCurrencies(w.usesForeignCurrencies(usesForeignCurrencies).moneySources(MoneySources())))
          }
          case _ => None
        }
      }
      case _ => None
    }
  }


  private def shouldAnswerForeignExchangeQuestions( msbServices: Set[BusinessMatchingMsbService],
                                                    register: ServiceChangeRegister,
                                                    isPreSubmission: Boolean,
                                                    msb: MoneyServiceBusiness,
                                                    edit: Boolean): Boolean = {
    foreignExchangeAddedPostSubmission(msbServices, register) ||
      (isPreSubmission && msbServices.contains(ForeignExchange) && (msb.fxTransactionsInNext12Months.isEmpty || !edit))
  }

  private def routing(msbServices: Set[BusinessMatchingMsbService],
                      register: ServiceChangeRegister,
                      msb: MoneyServiceBusiness,
                      edit: Boolean,
                      data: UsesForeignCurrencies)(implicit hc: HeaderCarrier, auth: AuthContext): Future[Result] = {
    statusService.isPreSubmission map { isPreSubmission =>
          if (data == UsesForeignCurrenciesYes) {
          Redirect(routes.MoneySourcesController.get(edit))
        } else if (shouldAnswerForeignExchangeQuestions(msbServices, register, isPreSubmission, msb, edit)) {
          Redirect(routes.FXTransactionsInNext12MonthsController.get(edit))
        } else {
          Redirect(routes.SummaryController.get())
        }
      }
    }
  }
