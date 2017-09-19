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
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.businessmatching.{MoneyServiceBusiness, BusinessMatching, BusinessActivities}
import scala.concurrent.Future
import views.html.businessmatching._

trait RegisterServicesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
          val form: Form2[BusinessActivities] = (for {
            businessMatching <- response
            businessActivities <- businessMatching.activities
          } yield Form2[BusinessActivities](businessActivities)) getOrElse EmptyForm

          Ok(register_services(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      import jto.validation.forms.Rules._
      Form2[BusinessActivities](request.body) match {
        case invalidForm: InvalidForm =>
          Future.successful(BadRequest(register_services(invalidForm, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](BusinessMatching.key,
              data.businessActivities.contains(MoneyServiceBusiness) match {
                case false => businessMatching.copy(activities = Some(data), msbServices = None)
                case true =>businessMatching.activities(data)
              }
            )
          } yield data.businessActivities.contains(MoneyServiceBusiness) match {
            case true => Redirect(routes.ServicesController.get(false))
            case false => Redirect(routes.SummaryController.get())
          }
      }
  }
}

object RegisterServicesController extends RegisterServicesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
