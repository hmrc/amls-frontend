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

package controllers.tcsp

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import models.tcsp._
import play.api.i18n.Messages
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.tcsp.summary
import play.api.Logger

class SummaryController @Inject()
(
  val dataCache: DataCacheConnector,
  val authConnector: AuthConnector,
  val serviceFlow: ServiceFlow,
  val statusService: StatusService
) extends BaseController {

  def sortProviders(data: Tcsp): List[String] = {

    val sortedList = (for {
      types <- data.tcspTypes
      providers <- Some(types.serviceProviders)
      labels <- Some(providers.collect {
          case provider if !provider.value.eq("05") => Messages(s"tcsp.service.provider.lbl.${provider.value}")
        }
      )
      specialCase <- Some(providers.collect {
          case provider if provider.value.eq("05") => Messages(s"tcsp.service.provider.lbl.05")
        }
      )
    } yield labels.toList.sorted ++ specialCase.toList).getOrElse(List())


    if (sortedList.isEmpty) {
      Logger.warn(s"[tcsp][SummaryController][sortProviders] - tcsp provider list is empty")
    }

    sortedList
  }

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        fetchModel map {
          case Some(data) => Ok(summary(data, sortProviders(data)))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(fetchModel)
          _ <- OptionT.liftF(dataCache.save[Tcsp](Tcsp.key, model.copy(hasAccepted = true)))
        } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Cannot update Tcsp")
  }

  private def fetchModel(implicit authContext: AuthContext, hc: HeaderCarrier) = dataCache.fetch[Tcsp](Tcsp.key)
}
