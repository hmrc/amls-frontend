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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.flowmanagement._
import services.ResponsiblePeopleService._
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import services.{ResponsiblePeopleService, StatusService}
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.add.which_fit_and_proper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WhichFitAndProperController @Inject()(
                                             authAction: AuthAction,
                                             implicit val dataCacheConnector: DataCacheConnector,
                                             val statusService: StatusService,
                                             val businessMatchingService: BusinessMatchingService,
                                             val responsiblePeopleService: ResponsiblePeopleService,
                                             val helper: AddBusinessTypeHelper,
                                             val router: Router[AddBusinessTypeFlowModel]
                                           ) extends DefaultBaseController with RepeatingSection {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          rp <- OptionT.liftF(responsiblePeopleService.getAll(request.credId))
          flowModel <- OptionT(dataCacheConnector.fetch[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key))
        } yield {
          val indexedRp = rp.zipWithIndex.exceptDeleted
          val form = flowModel.responsiblePeople.fold[Form2[ResponsiblePeopleFitAndProper]](EmptyForm)(Form2[ResponsiblePeopleFitAndProper])
          Ok(which_fit_and_proper(form, edit, indexedRp))
        }) getOrElse InternalServerError("")
  }

  //hasAlreadyPassedFitAndProper
  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[ResponsiblePeopleFitAndProper](request.body) match {
          case f: InvalidForm => responsiblePeopleService.getAll(request.credId) map { rp =>
            BadRequest(which_fit_and_proper(f, edit, rp.zipWithIndex.exceptInactive))
          }
          case ValidForm(_, data) => {
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              case Some(model) => model.responsiblePeople(Some(data))
            } flatMap {
              case Some(model) => router.getRoute(request.credId, WhichFitAndProperPageId, model, edit)
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
          }
        }
  }
}

