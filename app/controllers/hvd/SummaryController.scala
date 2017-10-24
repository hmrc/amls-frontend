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

import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.hvd.Hvd
import services.StatusService
import utils.ControllerHelper
import views.html.hvd.summary
import forms.EmptyForm
import models.businessmatching.HighValueDealing
import play.api.Play
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class SummaryController @Inject()
(
  val dataCache: DataCacheConnector,
  val authConnector: AuthConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow
) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          hvd <- dataCache.fetch[Hvd](Hvd.key)
          isEditable <- ControllerHelper.allowedToEdit
        } yield hvd match {
          case Some(data) => Ok(summary(EmptyForm, data, isEditable))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          hvd <- OptionT(dataCache.fetch[Hvd](Hvd.key))
          _ <- OptionT.liftF(dataCache.save[Hvd](Hvd.key, hvd.copy(hasAccepted = true)))
          preSubmission <- OptionT.liftF(statusService.isPreSubmission)
          isNewActivity <- OptionT.liftF(serviceFlow.inNewServiceFlow(HighValueDealing))
        } yield (preSubmission, isNewActivity) match {
          case (false, true) => Redirect(controllers.businessmatching.updateservice.routes.NewServiceInformationController.get())
          case _ => Redirect(controllers.routes.RegistrationProgressController.get)
        }) getOrElse InternalServerError("Could not update HVD")
  }
}
