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

package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.BusinessMatching
import models.renewal.{UpdateAnyInformation, UpdateAnyInformationNo, UpdateAnyInformationYes}
import services.{ProgressService, RenewalService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

@Singleton
class UpdateAnyInformationController @Inject()(
                                                val authConnector: AuthConnector,
                                                val dataCacheConnector: DataCacheConnector,
                                                val renewalService: RenewalService,
                                                val progressService: ProgressService
                                              ) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        cache <- OptionT(dataCacheConnector.fetchAll)
        renewals <- OptionT.liftF(renewalService.getSection)
      } yield {
        val variationSections = progressService.sections(cache).filter(_.name != BusinessMatching.messageKey)
        if(renewalService.canSubmit(renewals, variationSections)){
          Ok(views.html.update_any_information(EmptyForm, routes.UpdateAnyInformationController.post(), "summary.renewal"))
        } else {
          NotFound(notFoundView)
        }
      }) getOrElse InternalServerError

  }

  def post = Authorised.async{
    implicit authContext => implicit request =>
      Form2[UpdateAnyInformation](request.body) match {
        case ValidForm(_, data) => data match {
          case UpdateAnyInformationYes => Future.successful(Redirect(controllers.renewal.routes.RenewalProgressController.get().url))
          case UpdateAnyInformationNo => Future.successful(Redirect(controllers.declaration.routes.WhoIsRegisteringController.get().url))
        }
        case f:InvalidForm => Future.successful(
          BadRequest(views.html.update_any_information(f, routes.UpdateAnyInformationController.post(), "summary.renewal"))
        )
      }
  }

}
