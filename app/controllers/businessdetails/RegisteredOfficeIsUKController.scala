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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import models.businessdetails._
import play.api.mvc.MessagesControllerComponents
import utils.{AuthAction, DateOfChangeHelper}
import views.html.businessdetails._

import scala.concurrent.Future


class RegisteredOfficeIsUKController @Inject ()(
                                                val dataCacheConnector: DataCacheConnector,
                                                val authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                val cc: MessagesControllerComponents,
                                                registered_office_is_uk: registered_office_is_uk) extends AmlsBaseController(ds, cc) with DateOfChangeHelper {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
          response =>
            response.flatMap(businessDetails =>
              businessDetails.registeredOfficeIsUK.map(isUk => isUk.isUK)
                .orElse(businessDetails.registeredOffice.flatMap(ro => ro.isUK)))
              .map(isUk => Ok(registered_office_is_uk(Form2[RegisteredOfficeIsUK](RegisteredOfficeIsUK(isUk)), edit)) )
              .getOrElse(Ok(registered_office_is_uk(EmptyForm, edit)))
        }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
        Form2[RegisteredOfficeIsUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office_is_uk(f, edit)))
          case ValidForm(_, data) =>
            for {
              businessDetails: Option[BusinessDetails] <- dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key)
              _ <- dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails. registeredOfficeIsUK(data))
              _ <- if (isUkHasChanged(businessDetails.registeredOffice, isUk = data)) { dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key,
                businessDetails.copy(registeredOffice = None)) } else { Future.successful(None) }
            } yield {
              data match {
                case RegisteredOfficeIsUK(true) => Redirect(routes.RegisteredOfficeUKController.get(edit))
                case RegisteredOfficeIsUK(false) => Redirect(routes.RegisteredOfficeNonUKController.get(edit))
              }
            }
        }
  }
  def isUkHasChanged(address: Option[RegisteredOffice], isUk: RegisteredOfficeIsUK):Boolean = {
    (address, isUk) match {
      case (Some(RegisteredOfficeUK(_, _, _, _, _, _)), RegisteredOfficeIsUK(false)) => true
      case (Some(RegisteredOfficeNonUK(_, _, _, _, _, _)), RegisteredOfficeIsUK(true)) => true
      case _ => false
    }
  }
}
