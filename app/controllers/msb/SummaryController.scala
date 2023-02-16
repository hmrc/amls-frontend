/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import models.businessmatching.updateservice.ServiceChangeRegister
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import services.businessmatching.ServiceFlow
import utils.AuthAction

import views.html.msb.summary

class SummaryController @Inject()(authAction: AuthAction,
                                  val ds: CommonPlayDependencies,
                                  implicit val dataCache: DataCacheConnector,
                                  implicit val statusService: StatusService,
                                  implicit val serviceFlow: ServiceFlow,
                                  val cc: MessagesControllerComponents,
                                  summary: summary) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
    implicit request =>
      dataCache.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
            register <- cache.getEntry[ServiceChangeRegister](ServiceChangeRegister.key) orElse Some(ServiceChangeRegister())
          } yield {
            Ok(summary(msb, businessMatching.msbServices, register))
          }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get)
      }
  }


  def post() = authAction.async {
    implicit request =>
      for {
        model <- dataCache.fetch[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key)
        _ <- dataCache.save[MoneyServiceBusiness](request.credId, MoneyServiceBusiness.key, model.copy(hasAccepted = true))
      } yield Redirect(controllers.routes.RegistrationProgressController.get)
  }

}
