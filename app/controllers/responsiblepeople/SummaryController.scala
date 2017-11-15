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

package controllers.responsiblepeople

import javax.inject.Inject

import cats.data.OptionT
import cats.implicits._
import config.AppConfig
import connectors.DataCacheConnector
import controllers.BaseController
import models.responsiblepeople.ResponsiblePeople
import models.responsiblepeople.ResponsiblePeople.{flowChangeOfficer, flowFromDeclaration}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, DeclarationHelper}
import views.html.responsiblepeople._
import models.responsiblepeople.ResponsiblePeople.FilterUtils
import scala.concurrent.Future

class SummaryController @Inject()(
                                   val dataCacheConnector: DataCacheConnector,
                                   val authConnector: AuthConnector,
                                   val statusService: StatusService,
                                   config: AppConfig
                                 ) extends BaseController {

  def get(flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      fetchModel map {
        case Some(data) =>
          val hasNonUKResident = ControllerHelper.hasNonUkResident(Some(data))
          Ok(check_your_answers(data, flow, hasNonUKResident))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
    }

  def post(flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      flow match {
        case Some(`flowFromDeclaration`) => redirectFromDeclarationFlow()
        case Some(`flowChangeOfficer`) => Future.successful(Redirect(controllers.changeofficer.routes.NewOfficerController.get()))
        case None => {
          (for {
            model <- OptionT(fetchModel)
            _ <- OptionT.liftF(dataCacheConnector.save(ResponsiblePeople.key, model.filterEmpty.map(_.copy(hasAccepted = true))))
          } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Cannot update ResponsiblePeople")
        }
      }
    }

  private def redirectFromDeclarationFlow()(implicit hc: HeaderCarrier, authContext: AuthContext) =
    for {
      status <- statusService.getStatus
      hasNominatedOfficer <- ControllerHelper.hasNominatedOfficer(dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key))
    } yield Redirect(DeclarationHelper.routeDependingOnNominatedOfficer(hasNominatedOfficer, status, config.showFeesToggle))

  private def fetchModel(implicit authContext: AuthContext, hc: HeaderCarrier) =
    dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
}