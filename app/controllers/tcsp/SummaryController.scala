/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.tcsp._
import play.api.i18n.Messages
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.HeaderCarrier
import views.html.tcsp.summary
import play.api.Logger
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction

class SummaryController @Inject()(
                                  val dataCache: DataCacheConnector,
                                  val authAction: AuthAction,
                                  val ds: CommonPlayDependencies,
                                  val serviceFlow: ServiceFlow,
                                  val statusService: StatusService,
                                  val cc: MessagesControllerComponents,
                                  val summary: summary,
                                  implicit val error: views.html.error) extends AmlsBaseController(ds, cc) {

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

  def get = authAction.async {
      implicit request =>
        fetchModel(request.credId) map {
          case Some(data) if data.copy(hasAccepted = true).isComplete => Ok(summary(data, sortProviders(data)))
          case _ => Redirect(controllers.routes.RegistrationProgressController.get())
        }
  }

  def post = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(fetchModel(request.credId))
          _ <- OptionT.liftF(dataCache.save[Tcsp](request.credId, Tcsp.key, model.copy(hasAccepted = true)))
        } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Cannot update Tcsp")
  }

  private def fetchModel(credId: String)(implicit hc: HeaderCarrier) = dataCache.fetch[Tcsp](credId, Tcsp.key)
}
