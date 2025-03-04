/*
 * Copyright 2024 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.MsbSubSectorsFormProvider
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router2
import utils.AuthAction
import views.html.businessmatching.MsbServicesView

import javax.inject.Inject
import scala.concurrent.Future

class MsbSubSectorsController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val dataCacheConnector: DataCacheConnector,
  val router: Router2[ChangeSubSectorFlowModel],
  val businessMatchingService: BusinessMatchingService,
  val statusService: StatusService,
  val helper: ChangeSubSectorHelper,
  val config: ApplicationConfig,
  val cc: MessagesControllerComponents,
  formProvider: MsbSubSectorsFormProvider,
  services: MsbServicesView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    (for {
      bm     <- businessMatchingService.getModel(request.credId)
      status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
    } yield {
      val form = bm.msbServices.fold(formProvider())(services => formProvider().fill(services.msbServices.toSeq))
      Ok(services(form, edit, bm.preAppComplete, statusService.isPreSubmission(status), config.fxEnabledToggle))
    }) getOrElse Ok(services(formProvider(), edit, fxEnabledToggle = config.fxEnabledToggle))
  }

  def post(edit: Boolean = false, includeCompanyNotRegistered: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(services(formWithErrors, edit, fxEnabledToggle = config.fxEnabledToggle))),
          data =>
            dataCacheConnector.update[ChangeSubSectorFlowModel](request.credId, ChangeSubSectorFlowModel.key) {
              _.getOrElse(ChangeSubSectorFlowModel()).copy(subSectors = Some(data.toSet))
            } flatMap {
              case Some(m @ ChangeSubSectorFlowModel(Some(set), _)) if !(set contains TransmittingMoney) =>
                helper.updateSubSectors(request.credId, m) flatMap { _ =>
                  router.getRoute(request.credId, SubSectorsPageId, m, edit, includeCompanyNotRegistered)
                }
              case Some(updatedModel)                                                                    =>
                router.getRoute(request.credId, SubSectorsPageId, updatedModel, edit, includeCompanyNotRegistered)
            }
        )
  }
}
