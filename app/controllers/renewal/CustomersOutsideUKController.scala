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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.renewal.{CustomersOutsideUK, Renewal}
import play.api.mvc.MessagesControllerComponents
import services.{AutoCompleteService, RenewalService}
import utils.{AuthAction, ControllerHelper}
import views.html.renewal._

import scala.concurrent.Future


@Singleton
class CustomersOutsideUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                             val authAction: AuthAction,
                                             val ds: CommonPlayDependencies,
                                             val renewalService: RenewalService,
                                             val autoCompleteService: AutoCompleteService,
                                             val cc: MessagesControllerComponents,
                                             customers_outside_uk: customers_outside_uk) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        renewalService.getRenewal(request.credId).map {
          response =>
            val form: Form2[CustomersOutsideUK] = (for {
              renewal <- response
              customers <- renewal.customersOutsideUK
            } yield Form2[CustomersOutsideUK](customers)).getOrElse(EmptyForm)
            Ok(customers_outside_uk(form, edit, autoCompleteService.getCountries))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[CustomersOutsideUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(customers_outside_uk(alignFormDataWithValidationErrors(f), edit, autoCompleteService.getCountries)))
          case ValidForm(_, data) => {
            dataCacheConnector.fetchAll(request.credId).flatMap { optionalCache =>
              (for {
                cache <- optionalCache
                businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                renewal <- cache.getEntry[Renewal](Renewal.key)
              } yield {
                renewalService.updateRenewal(request.credId, renewal.customersOutsideUK(data)) map {
                  _ =>
                    (edit, businessMatching) match {
                      case (true, _) => Redirect(routes.SummaryController.get())
                      case (false, bm) if bm.activities.isDefined => bm.activities.get.businessActivities match {
                        case x if x.contains(HighValueDealing) => Redirect(routes.PercentageOfCashPaymentOver15000Controller.get())
                        case _ => Redirect(routes.SummaryController.get())
                      }
                    }
                }
              }) getOrElse Future.successful(InternalServerError("Unable to get data from the cache"))
            }
          }
        }
  }

  def alignFormDataWithValidationErrors(form: InvalidForm): InvalidForm =
    ControllerHelper.stripEmptyValuesFromFormWithArray(form, "countries", index => index / 2)
}


