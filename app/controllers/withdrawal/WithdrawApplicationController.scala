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

package controllers.withdrawal

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.BusinessName
import views.html.withdrawal.withdraw_application

import scala.concurrent.Future

class WithdrawApplicationController @Inject()(
                                               val authConnector: AuthConnector,
                                               implicit val amls: AmlsConnector,
                                               implicit val dc: DataCacheConnector,
                                               statusService: StatusService) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        val maybeProcessingDate = for {
          status <- OptionT.liftF(statusService.getDetailedStatus)
          response <- OptionT.fromOption[Future](status._2)
        } yield response.processingDate

        (for {
          cache <- OptionT(dc.fetch[BusinessMatching](BusinessMatching.key))
          details <- OptionT.fromOption[Future](cache.reviewDetails)
          processingDate <- maybeProcessingDate
          name <- BusinessName.getNameFromAmls(details.safeId)
        } yield Ok(withdraw_application(name, processingDate))) getOrElse InternalServerError("Unable to show the withdrawal page")
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Redirect(routes.WithdrawalReasonController.get()))
  }

}
