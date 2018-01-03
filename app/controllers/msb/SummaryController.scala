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

package controllers.msb

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness => MsbActivity}
import models.moneyservicebusiness.MoneyServiceBusiness
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.summary

import scala.concurrent.Future

class SummaryController @Inject()
(
  val dataCache: DataCacheConnector,
  implicit val statusService: StatusService,
  val authConnector: AuthConnector,
  implicit val serviceFlow: ServiceFlow
) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
          } yield {
            ControllerHelper.allowedToEdit(MsbActivity) map(x => Ok(summary(msb, businessMatching.msbServices,x)))
          }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
      }
  }


  def post() = Authorised.async{
    implicit authContext => implicit request =>
      (for {
        model <- OptionT(dataCache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key))
        _ <- OptionT.liftF(dataCache.save[MoneyServiceBusiness](MoneyServiceBusiness.key, model.copy(hasAccepted = true)))
        preSubmission <- OptionT.liftF(statusService.isPreSubmission)
        isNewActivity <- OptionT.liftF(serviceFlow.inNewServiceFlow(models.businessmatching.MoneyServiceBusiness))
      } yield (preSubmission, isNewActivity) match {
        case (false, true) => Redirect(controllers.businessmatching.updateservice.routes.NewServiceInformationController.get())
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }) getOrElse InternalServerError("Cannot update MoneyServiceBusiness")
  }

}
