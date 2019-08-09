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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{BusinessMatching, CompanyRegistrationNumber}
import views.html.businessmatching.company_registration_number
import services.StatusService
import services.businessmatching.BusinessMatchingService
import utils.AuthAction

import scala.concurrent.Future

class CompanyRegistrationNumberController@Inject()(authAction: AuthAction,
                                                   val dataCacheConnector: DataCacheConnector,
                                                   val statusService: StatusService,
                                                   val businessMatchingService:BusinessMatchingService) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          bm <- businessMatchingService.getModel(request.credId)
          status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId,request.credId))
        } yield {
          val form: Form2[CompanyRegistrationNumber] = bm.companyRegistrationNumber map
            Form2[CompanyRegistrationNumber] getOrElse EmptyForm
            Ok(company_registration_number(form, edit, bm.hasAccepted , statusService.isPreSubmission(status)))
        }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
    }


  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[CompanyRegistrationNumber](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(company_registration_number(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
            _ <- dataCacheConnector.save[BusinessMatching](request.credId, BusinessMatching.key,
              businessMatching.companyRegistrationNumber(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.RegisterServicesController.get())
          }
      }
    }
  }
}

