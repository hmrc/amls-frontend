/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import forms.businessactivities.TaxMattersFormProvider
import models.businessactivities.BusinessActivities
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{AuthAction, ControllerHelper}
import views.html.businessactivities.TaxMattersView

class TaxMattersController @Inject() (val dataCacheConnector: DataCacheConnector,
                                      val authAction: AuthAction,
                                      val ds: CommonPlayDependencies,
                                      val cc: MessagesControllerComponents,
                                      view: TaxMattersView,
                                      formProvider: TaxMattersFormProvider,
                                      implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
          case Some(BusinessActivities(_,_,_,_,_,_,_,_,_,_,_,Some(whoIsYourAccountant), Some(taxMatters),_,_,_))
          => Ok(view(formProvider().fill(taxMatters), edit, whoIsYourAccountant.names.map(name => name.accountantsName)))
          case Some(BusinessActivities(_,_,_,_,_,_,_,_,_,_,_,Some(whoIsYourAccountant), _,_,_,_))
          => Ok(view(formProvider(), edit, whoIsYourAccountant.names.map(name => name.accountantsName)))
          case _ => NotFound(notFoundView)
      }
  }

  def post(edit : Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider().bindFromRequest.fold(
        formWithErrors => {
          dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map { ba =>
            BadRequest(view(formWithErrors, edit, ControllerHelper.accountantName(ba)))
          }
        },
        data =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId,
              BusinessActivities.key,
              businessActivities.taxMatters(Some(data))
            )
          } yield Redirect(routes.SummaryController.get)
      )
  }
}