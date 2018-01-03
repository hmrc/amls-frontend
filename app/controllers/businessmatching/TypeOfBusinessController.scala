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

package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{TypeOfBusiness, BusinessMatching}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.type_of_business

import scala.concurrent.Future

trait TypeOfBusinessController extends BaseController {

  private[controllers] def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[TypeOfBusiness] = (for {
            businessMatching <- response
            business <- businessMatching.typeOfBusiness
          } yield Form2[TypeOfBusiness](business)).getOrElse(EmptyForm)
          Ok(type_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TypeOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(type_of_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              businessMatching.typeOfBusiness(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.RegisterServicesController.get())
          }
      }
    }
  }
}

object TypeOfBusinessController extends TypeOfBusinessController {
  // $COVERAGE-OFF$
  override private[controllers] def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
