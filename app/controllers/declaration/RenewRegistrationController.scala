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

package controllers.declaration

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import models.declaration.{RenewRegistration, RenewRegistrationNo, RenewRegistrationYes}
import play.api.mvc.Result
import services.ProgressService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction

import scala.concurrent.{ExecutionContext, Future}

class RenewRegistrationController @Inject()(val dataCacheConnector: DataCacheConnector,
                                            val authAction: AuthAction,
                                            val progressService: ProgressService) extends DefaultBaseController {

  def get() = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[RenewRegistration](request.credId, RenewRegistration.key) map {
        renewRegistration =>
          val form = (for {
            renew <- renewRegistration
          } yield Form2[RenewRegistration](renew)).getOrElse(EmptyForm)
          Ok(views.html.declaration.renew_registration(form))
      }
  }

  def post() = authAction.async {
    implicit request => {
      Form2[RenewRegistration](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.declaration.renew_registration(f)))
        case ValidForm(_, data) =>
          dataCacheConnector.save[RenewRegistration](request.credId, RenewRegistration.key, data)
          redirectDependingOnResponse(data, request.amlsRefNumber, request.accountTypeId, request.credId)
      }
    }
  }

  private def redirectDependingOnResponse(data: RenewRegistration,
                                          amlsRefNo: Option[String],
                                          accountTypeId: (String, String),
                                          credId: String)
                                         (implicit hc: HeaderCarrier, ec: ExecutionContext) = data match {
    case RenewRegistrationYes => Future.successful(Redirect(controllers.renewal.routes.WhatYouNeedController.get()))
    case RenewRegistrationNo  => resolveDeclarationDest(amlsRefNo, accountTypeId, credId)
  }

  private def resolveDeclarationDest(amlsRefNo: Option[String],
                                     accountTypeId: (String, String),
                                     credId: String)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    progressService.getSubmitRedirect(amlsRefNo, accountTypeId, credId) map {
      case Some(url) => Redirect(url)
      case _ => InternalServerError("Could not get data for redirect")
    }
  }
}
