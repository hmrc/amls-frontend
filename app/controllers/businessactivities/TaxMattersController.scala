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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities._

class TaxMattersController @Inject() (val dataCacheConnector: DataCacheConnector,
                                      val authAction: AuthAction
                                     ) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
          case Some(BusinessActivities(_,_,_,_,_,_,_,_,_,_,_,Some(whoIsYourAccountant), Some(taxMatters),_,_,_))
          => Ok(tax_matters(Form2[TaxMatters](taxMatters), edit, whoIsYourAccountant.names.map(name => name.accountantsName)))
          case Some(BusinessActivities(_,_,_,_,_,_,_,_,_,_,_,Some(whoIsYourAccountant), _,_,_,_))
          => Ok(tax_matters(EmptyForm, edit, whoIsYourAccountant.names.map(name => name.accountantsName)))
          case _ => NotFound(notFoundView)
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[TaxMatters](request.body) match {
        case f: InvalidForm =>
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { ba =>
          BadRequest(tax_matters(f, edit, ControllerHelper.accountantName(ba)))
          }
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId,
              BusinessActivities.key,
              businessActivities.taxMatters(Some(data))
            )
          } yield Redirect(routes.SummaryController.get())
      }
  }
}