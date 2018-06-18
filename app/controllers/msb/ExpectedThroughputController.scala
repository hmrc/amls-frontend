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

package controllers.msb

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching.{TransmittingMoney, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness.{ExpectedThroughput, MoneyServiceBusiness}
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.expected_throughput

import scala.concurrent.Future

class ExpectedThroughputController @Inject() (val authConnector: AuthConnector,
                                              implicit val dataCacheConnector: DataCacheConnector,
                                              implicit val statusService: StatusService,
                                              implicit val serviceFlow: ServiceFlow
                                             ) extends BaseController {


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit(MsbActivity, Some(TransmittingMoney)) flatMap {
        case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[ExpectedThroughput] = (for {
              msb <- response
              expectedThroughput <- msb.throughput
            } yield Form2[ExpectedThroughput](expectedThroughput)).getOrElse(EmptyForm)
            Ok(expected_throughput(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ExpectedThroughput](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(expected_throughput(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.throughput(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.BranchesOrAgentsController.get())
          }
      }
    }
  }
}
