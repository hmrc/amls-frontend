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

package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait TaxMattersController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key) map {
        response =>
          val form: Form2[TaxMatters] = (for {
            businessActivities <- response
            taxMatters <- businessActivities.taxMatters
          } yield Form2[TaxMatters](taxMatters)).getOrElse(EmptyForm)
          Ok(tax_matters(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TaxMatters](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(tax_matters(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](
              BusinessActivities.key,
              businessActivities.taxMatters(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
  }
}

object TaxMattersController extends TaxMattersController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
