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

package controllers.businessmatching.updateservice

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, ValidForm}
import jto.validation.{From, Rule, Valid}
import jto.validation.forms.UrlFormEncoded
import models.businessmatching.updateservice.UpdateService
import play.api.i18n.MessagesApi
import services.StatusService
import services.businessmatching.{BusinessMatchingService, ServiceFlow}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.updateservice.new_service_information
import scala.concurrent.Future


class NewServiceInformationController @Inject()
(
  val authConnector: AuthConnector,
  val dataCacheConnector: DataCacheConnector,
  val businessMatchingService: BusinessMatchingService,
  val serviceFlow: ServiceFlow,
  val messages: MessagesApi
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext => implicit request => {
        for {
          next <- serviceFlow.next
          _ <- OptionT.liftF(businessMatchingService.clearSection(next.activity))
        } yield Ok(new_service_information(next.activity, next.url))
      } getOrElse Redirect(controllers.businessmatching.updateservice.routes.UpdateAnyInformationController.get())
  }

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      for {
        _ <- OptionT.liftF(serviceFlow.setInServiceFlowFlag(true))
        form <- OptionT.fromOption[Future](request.body.asFormUrlEncoded)
        url <- OptionT.fromOption[Future](form.get("redirectUrl"))
      } yield Redirect(url.head)
    } getOrElse InternalServerError("Unable to configure UpdateService")
  }
}
