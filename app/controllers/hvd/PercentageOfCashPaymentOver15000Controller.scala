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

package controllers.hvd

import javax.inject.Inject

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.HighValueDealing
import models.hvd.{Hvd, PercentageOfCashPaymentOver15000, ReceiveCashPayments}
import play.api.Play
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.hvd.{percentage, receiving}

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000Controller @Inject()
(
  val dataCacheConnector: DataCacheConnector,
  implicit val serviceFlow: ServiceFlow,
  implicit val statusService: StatusService,
  val authConnector: AuthConnector
) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit(HighValueDealing) flatMap {
        case true =>
          dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[PercentageOfCashPaymentOver15000] = (for {
              hvd <- response
              percentageOfCashPaymentOver15000 <- hvd.percentageOfCashPaymentOver15000
            } yield Form2[PercentageOfCashPaymentOver15000](percentageOfCashPaymentOver15000)).getOrElse(EmptyForm)
            Ok(percentage(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

    def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PercentageOfCashPaymentOver15000](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(percentage(f, edit)))
        case ValidForm(_, data) =>
          for {
            hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
            _ <- dataCacheConnector.save[Hvd](Hvd.key,
              hvd.percentageOfCashPaymentOver15000(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}
