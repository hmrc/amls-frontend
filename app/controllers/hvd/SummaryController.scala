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

package controllers.hvd

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import javax.inject.Inject
import models.businessmatching.HighValueDealing
import models.hvd.Hvd
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.hvd.summary

class SummaryController @Inject() (val authConnector: AuthConnector,
                                   implicit val dataCache: DataCacheConnector,
                                   implicit val statusService: StatusService,
                                   implicit val serviceFlow: ServiceFlow
                                  ) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          hvd <- dataCache.fetch[Hvd](Hvd.key)
        } yield hvd match {
          case Some(data) => Ok(summary(EmptyForm, data))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          hvd <- OptionT(dataCache.fetch[Hvd](Hvd.key))
          _ <- OptionT.liftF(dataCache.save[Hvd](Hvd.key, hvd.copy(hasAccepted = true)))
        } yield Redirect(controllers.routes.RegistrationProgressController.get)) getOrElse InternalServerError("Could not update HVD")
  }
}
