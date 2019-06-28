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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessdetails._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.DateOfChangeHelper
import views.html.businessdetails._

import scala.concurrent.Future

class RegisteredOfficeIsUKController @Inject ()(
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector
                                            ) extends BaseController with DateOfChangeHelper {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[BusinessDetails](BusinessDetails.key) map {
          response =>
            response.flatMap(businessDetails =>
              businessDetails.registeredOfficeIsUK.map(isUk => isUk.isUK)
                .orElse(businessDetails.registeredOffice.flatMap(ro => ro.isUK)))
              .map(isUk => Ok(registered_office_is_uk(Form2[RegisteredOfficeIsUK](RegisteredOfficeIsUK(isUk)), edit)) )
              .getOrElse(Ok(registered_office_is_uk(EmptyForm, edit)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        Form2[RegisteredOfficeIsUK](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office_is_uk(f, edit)))
          case ValidForm(_, data) =>
            for {
              businessDetails <- dataCacheConnector.fetch[BusinessDetails](BusinessDetails.key)
              _ <- dataCacheConnector.save[BusinessDetails](BusinessDetails.key, businessDetails.registeredOfficeIsUK(data))
            } yield {
              data match {
                case RegisteredOfficeIsUK(true) => Redirect(routes.RegisteredOfficeUKController.get(edit))
                case RegisteredOfficeIsUK(false) => Redirect(routes.RegisteredOfficeNonUKController.get(edit))
              }
            }
        }
  }
}
