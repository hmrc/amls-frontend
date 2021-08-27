/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.moneyservicebusiness.{ExpectedThroughput, MoneyServiceBusiness}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction

import views.html.msb.expected_throughput

import scala.concurrent.Future

class ExpectedThroughputController @Inject() (authAction: AuthAction,
                                              val ds: CommonPlayDependencies,
                                              implicit val dataCacheConnector: DataCacheConnector,
                                              implicit val statusService: StatusService,
                                              implicit val serviceFlow: ServiceFlow,
                                              val cc: MessagesControllerComponents,
                                              expected_throughput: expected_throughput) extends AmlsBaseController(ds, cc) {


  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[ExpectedThroughput] = (for {
            msb <- response
            expectedThroughput <- msb.throughput
          } yield Form2[ExpectedThroughput](expectedThroughput)).getOrElse(EmptyForm)
          Ok(expected_throughput(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[ExpectedThroughput](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(expected_throughput(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key,
              msb.throughput(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get)
            case false => Redirect(routes.BranchesOrAgentsController.get())
          }
      }
    }
  }
}
