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

package controllers.estateagentbusiness

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import javax.inject.Inject
import models.estateagentbusiness.EstateAgentBusiness
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.estateagentbusiness._

class SummaryController @Inject()
(
  val dataCache: DataCacheConnector,
  val authConnector: AuthConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        case Some(data) =>
          Ok(summary(EmptyForm, data))
        case _ =>
          Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        eab <- OptionT(dataCache.fetch[EstateAgentBusiness](EstateAgentBusiness.key))
        _ <- OptionT.liftF(dataCache.save[EstateAgentBusiness](EstateAgentBusiness.key, eab.copy(hasAccepted = true)))
      } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Could not update EstateAgentBusiness")

  }
}
