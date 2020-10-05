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

package controllers.businessactivities

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import play.api.mvc.MessagesControllerComponents
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, WhoIsYourAccountantName}
import services.AutoCompleteService
import utils.AuthAction
import views.html.businessactivities.who_is_your_accountant


import scala.concurrent.Future

class WhoIsYourAccountantNameController @Inject()(val dataCacheConnector: DataCacheConnector,
                                              val autoCompleteService: AutoCompleteService,
                                              val authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              val cc: MessagesControllerComponents,
                                                  who_is_your_accountant: who_is_your_accountant) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key) map {
        response =>
          val form = (for {
            businessActivities <- response
            whoIsYourAccountant <- businessActivities.whoIsYourAccountant.flatMap(acc => acc.names)
          } yield {
            Form2[WhoIsYourAccountantName](whoIsYourAccountant)
          }).getOrElse(EmptyForm)
          Ok(who_is_your_accountant(form, edit))
      }
  }

  def post(edit : Boolean = false) = authAction.async {
    implicit request =>
      Form2[WhoIsYourAccountantName](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(who_is_your_accountant(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivity.whoIsYourAccountant(businessActivity.flatMap(ba => ba.whoIsYourAccountant).map(acc => acc.copy(names = Option(data))))
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get())
          } else {
            Redirect(routes.WhoIsYourAccountantIsUkController.get())
          }
        }
      }
  }
}
