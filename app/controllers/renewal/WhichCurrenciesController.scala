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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.renewal.{Renewal, WhichCurrencies}
import play.api.mvc.MessagesControllerComponents
import services.RenewalService
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.which_currencies

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class WhichCurrenciesController @Inject()(val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          renewalService: RenewalService,
                                          dataCacheConnector: DataCacheConnector,
                                          val cc: MessagesControllerComponents,
                                          which_currencies: which_currencies) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        val block = for {
          renewal <- OptionT(renewalService.getRenewal(request.credId))
          whichCurrencies <- OptionT.fromOption[Future](renewal.whichCurrencies)
        } yield {
          Ok(which_currencies(Form2[WhichCurrencies](whichCurrencies), edit))
        }

        block getOrElse Ok(which_currencies(EmptyForm, edit))

  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[WhichCurrencies](request.body) match {
          case f: InvalidForm => Future.successful(BadRequest(which_currencies(alignFormDataWithValidationErrors(f), edit)))
          case ValidForm(_, model) =>
            dataCacheConnector.fetchAll(request.credId).flatMap {
              optMap =>
                val result = for {
                  cacheMap <- optMap
                  renewal <- cacheMap.getEntry[Renewal](Renewal.key)
                } yield {
                  renewalService.updateRenewal(request.credId, updateWhichCurrencies(renewal, model)) map { _ =>
                    edit match {
                      case true => Redirect(routes.SummaryController.get())
                      case _ => Redirect(routes.UsesForeignCurrenciesController.get())
                    }
                  }

                }
                result getOrElse Future.failed(new Exception("Unable to retrieve sufficient data"))
            }
        }
  }

  def alignFormDataWithValidationErrors(form: InvalidForm): InvalidForm =
    ControllerHelper.stripEmptyValuesFromFormWithArray(form, "currencies")

  def updateWhichCurrencies(oldRenewal: Renewal, whichCurrencies: WhichCurrencies) = {
    oldRenewal.whichCurrencies match {
      case Some(wc) => {
        val newWc = wc.currencies(whichCurrencies.currencies)
        oldRenewal.whichCurrencies(newWc)
      }
      case None => {
        val newWc = WhichCurrencies(whichCurrencies.currencies)
        oldRenewal.whichCurrencies(newWc)
      }
    }
  }
}
