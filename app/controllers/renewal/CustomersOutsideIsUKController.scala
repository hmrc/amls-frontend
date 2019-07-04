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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.renewal.{CustomersOutsideIsUK, Renewal}
import services.{AutoCompleteService, RenewalService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class CustomersOutsideIsUKController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               val authConnector: AuthConnector,
                                               val renewalService: RenewalService,
                                               val autoCompleteService: AutoCompleteService
                                            ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        renewalService.getRenewal map {
          response =>
            val form: Form2[CustomersOutsideIsUK] = (for {
              renewal <- response
              customers <- renewal.customersOutsideIsUK
            } yield Form2[CustomersOutsideIsUK](customers)).getOrElse(EmptyForm)
            Ok(customers_outside_uk_isUK(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[CustomersOutsideIsUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(customers_outside_uk_isUK(f, edit)))
          case ValidForm(_, data) =>
            dataCacheConnector.fetchAll flatMap { optionalCache =>
              (for {
                cache <- optionalCache
                renewal <- cache.getEntry[Renewal](Renewal.key)
              } yield {
                renewalService.updateRenewal(renewal.customersOutsideIsUK(data)) map {
                  _ => Redirect(routes.CustomersOutsideIsUKController.get())
                }
              }) getOrElse Future.successful(InternalServerError("Unable to get data from the cache"))
            }
        }
  }

}


