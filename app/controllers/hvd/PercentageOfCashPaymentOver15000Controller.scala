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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import javax.inject.Inject
import models.hvd.{Hvd, PercentageOfCashPaymentOver15000}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction
import scala.concurrent.ExecutionContext.Implicits.global
import views.html.hvd.percentage

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000Controller @Inject() (val authAction: AuthAction,
                                                            val ds: CommonPlayDependencies,
                                                            implicit val dataCacheConnector: DataCacheConnector,
                                                            implicit val serviceFlow: ServiceFlow,
                                                            implicit val statusService: StatusService,
                                                            val cc: MessagesControllerComponents,
                                                            percentage: percentage) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[Hvd](request.credId, Hvd.key) map {
      response =>
        val form: Form2[PercentageOfCashPaymentOver15000] = (for {
          hvd <- response
          percentageOfCashPaymentOver15000 <- hvd.percentageOfCashPaymentOver15000
        } yield Form2[PercentageOfCashPaymentOver15000](percentageOfCashPaymentOver15000)).getOrElse(EmptyForm)
        Ok(percentage(form, edit))
    }
  }

    def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[PercentageOfCashPaymentOver15000](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(percentage(f, edit)))
        case ValidForm(_, data) =>
          for {
            hvd <- dataCacheConnector.fetch[Hvd](request.credId, Hvd.key)
            _ <- dataCacheConnector.save[Hvd](request.credId, Hvd.key,
              hvd.percentageOfCashPaymentOver15000(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}
