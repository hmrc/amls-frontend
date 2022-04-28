/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{BusinessMatching, TypeOfBusiness}
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.businessmatching.type_of_business

import scala.concurrent.Future

class TypeOfBusinessController @Inject()(val dataCacheConnector: DataCacheConnector,
                                         authAction: AuthAction,
                                         val ds: CommonPlayDependencies,
                                         val cc: MessagesControllerComponents,
                                         type_of_business: type_of_business) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key) map {
        response =>
          val form: Form2[TypeOfBusiness] = (for {
            businessMatching <- response
            business <- businessMatching.typeOfBusiness
          } yield Form2[TypeOfBusiness](business)).getOrElse(EmptyForm)
          Ok(type_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[TypeOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(type_of_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](request.credId, BusinessMatching.key,
              businessMatching.typeOfBusiness(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get)
            case false => Redirect(routes.RegisterServicesController.get())
          }
      }
    }
  }
}