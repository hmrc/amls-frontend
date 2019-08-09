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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import config.AppConfig
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import utils.AuthAction

import scala.concurrent.Future

class MsbSubSectorsController @Inject()(authAction: AuthAction,
                                        val dataCacheConnector: DataCacheConnector,
                                        val router: Router[ChangeSubSectorFlowModel],
                                        val businessMatchingService: BusinessMatchingService,
                                        val statusService:StatusService,
                                        val helper: ChangeSubSectorHelper,
                                        val config: AppConfig) extends DefaultBaseController {

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          bm <- businessMatchingService.getModel(request.credId)
          status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
        } yield {
          val form: Form2[BusinessMatchingMsbServices] = bm.msbServices map
            Form2[BusinessMatchingMsbServices] getOrElse EmptyForm
            Ok(views.html.businessmatching.services(form, edit, bm.preAppComplete, statusService.isPreSubmission(status), config.fxEnabledToggle))
        }) getOrElse Ok(views.html.businessmatching.services(EmptyForm, edit, fxEnabledToggle = config.fxEnabledToggle))
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        import jto.validation.forms.Rules._
        Form2[BusinessMatchingMsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.services(f, edit, fxEnabledToggle = config.fxEnabledToggle)))

          case ValidForm(_, data) =>
            dataCacheConnector.update[ChangeSubSectorFlowModel](request.credId, ChangeSubSectorFlowModel.key) {
              _.getOrElse(ChangeSubSectorFlowModel()).copy(subSectors = Some(data.msbServices))
            } flatMap {
              case Some(m@ChangeSubSectorFlowModel(Some(set), _)) if !(set contains TransmittingMoney) =>
                helper.updateSubSectors(request.credId, m) flatMap { _ => router.getRoute(request.credId, SubSectorsPageId, m) }
              case Some(updatedModel) =>
                router.getRoute(request.credId, SubSectorsPageId, updatedModel)
            }
        }
  }
}
