/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.businessmatching.{CompanyRegistrationNumber, BusinessMatching}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.company_registration_number

import scala.concurrent.Future

trait CompanyRegistrationNumberController extends BaseController {

  private[controllers] def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[CompanyRegistrationNumber] = (for {
            businessMatching <- response
            registrationNumber <- businessMatching.companyRegistrationNumber
          } yield Form2[CompanyRegistrationNumber](registrationNumber)).getOrElse(EmptyForm)

          Ok(company_registration_number(form, edit, response.fold(false)(_.hasAccepted)))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CompanyRegistrationNumber](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(company_registration_number(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              businessMatching.companyRegistrationNumber(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.RegisterServicesController.get())
          }
      }
    }
  }


}

object CompanyRegistrationNumberController extends CompanyRegistrationNumberController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] def dataCacheConnector: DataCacheConnector = DataCacheConnector
}
