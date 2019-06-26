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

package controllers.msb

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness.MoneyServiceBusiness
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.summary

class SummaryController @Inject()
(
  val authConnector: AuthConnector,
  implicit val dataCache: DataCacheConnector,
  implicit val statusService: StatusService,
  implicit val serviceFlow: ServiceFlow
) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
            register <- cache.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
          } yield {
            Ok(summary(msb, businessMatching.msbServices, register))
          }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }


  def post() = Authorised.async{
    implicit authContext => implicit request =>
      (for {
        model <- OptionT(dataCache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key))
        _ <- OptionT.liftF(dataCache.save[MoneyServiceBusiness](MoneyServiceBusiness.key, model.copy(hasAccepted = true)))
      } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Cannot update MoneyServiceBusiness")
  }

}
