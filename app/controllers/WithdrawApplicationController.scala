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

package controllers

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.AmlsConnector
import services.AuthEnrolmentsService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.withdraw_application

import scala.concurrent.Future

class WithdrawApplicationController @Inject()
(val authConnector: AuthConnector,
 amlsConnector: AmlsConnector,
 authService: AuthEnrolmentsService) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request => Future.successful(Ok(withdraw_application()))
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        regNumber <- OptionT(authService.amlsRegistrationNumber)
        _ <- OptionT.liftF(amlsConnector.withdraw(regNumber))
      } yield Redirect(routes.LandingController.get())) getOrElse InternalServerError("Unable to withdraw the application")
  }

}
