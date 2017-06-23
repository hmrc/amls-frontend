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

package controllers.withdrawal

import javax.inject.Inject

import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.withdrawal.WithdrawalReason
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.FeatureToggle
import views.html.withdrawal.withdrawal_reason

import scala.concurrent.Future

class WithdrawalReasonController @Inject()(
                                            val authConnector: AuthConnector,
                                            val dataCacheConnector: DataCacheConnector,
                                            amls: AmlsConnector,
                                            enrolments: AuthEnrolmentsService,
                                            statusService: StatusService) extends BaseController {

  def get = FeatureToggle(ApplicationConfig.allowWithdrawalToggle) {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[WithdrawalReason](WithdrawalReason.key) map {
          case Some(reason) => Ok(withdrawal_reason(Form2[WithdrawalReason](reason)))
          case _ => Ok(withdrawal_reason(EmptyForm))
        }
    }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Redirect(controllers.routes.LandingController.get()))
  }

}
