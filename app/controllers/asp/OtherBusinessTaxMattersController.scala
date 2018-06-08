/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.asp

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.asp.{Asp, OtherBusinessTaxMatters}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.asp.other_business_tax_matters

import scala.concurrent.Future

class OtherBusinessTaxMattersController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                  val authConnector: AuthConnector
                                                 ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[Asp](Asp.key) map {
          response =>
            val form: Form2[OtherBusinessTaxMatters] = (for {
              asp <- response
              otherTax <- asp.otherBusinessTaxMatters
            } yield Form2[OtherBusinessTaxMatters](otherTax)).getOrElse(EmptyForm)
            Ok(other_business_tax_matters(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[OtherBusinessTaxMatters](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(other_business_tax_matters(f, edit)))
          case ValidForm(_, data) =>
            for {
              asp <- dataCacheConnector.fetch[Asp](Asp.key)
              _ <- dataCacheConnector.save[Asp](Asp.key,
                asp.otherBusinessTaxMatters(data)
              )
            } yield Redirect(routes.SummaryController.get())
        }
      }
  }

}
