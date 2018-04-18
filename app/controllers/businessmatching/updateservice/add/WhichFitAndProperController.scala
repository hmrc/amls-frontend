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

package controllers.businessmatching.updateservice.add

import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.flowmanagement._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.{ResponsiblePeopleService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.add._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichFitAndProperController @Inject()(
                                             val authConnector: AuthConnector,
                                             implicit val dataCacheConnector: DataCacheConnector,
                                             val statusService: StatusService,
                                             val businessMatchingService: BusinessMatchingService,
                                             val responsiblePeopleService: ResponsiblePeopleService,
                                             val helper: UpdateServiceHelper,
                                             val router: Router[AddServiceFlowModel]
                                           ) extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        responsiblePeopleService.getActiveWithIndex map {
          case (rp) => Ok(which_fit_and_proper(EmptyForm, rp))
          case _ => InternalServerError("Unable to show the view")
        }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ResponsiblePeopleFitAndProper](request.body) match {
          case f: InvalidForm => responsiblePeopleService.getActiveWithIndex map { rp =>
            BadRequest(which_fit_and_proper(f, rp))
          }
          case ValidForm(_, data) => {
            responsiblePeopleService.updateResponsiblePeople(data) flatMap { _ =>
              dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key) flatMap {
                case Some(model) => {
                  router.getRoute(WhichFitAndProperPageId, model)
                }
                case _ => Future.successful(InternalServerError("Cannot retrieve data"))
              }
            }
          }
          case _ => Future.successful(InternalServerError("Cannot retrieve form data"))
        }
      }

}