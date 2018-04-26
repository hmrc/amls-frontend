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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddServiceFlowModel, SubServicesPageId}
import services.StatusService
import models.businessmatching.{BusinessActivity, BusinessActivities => BusinessMatchingActivities}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.services
import views.html.businessmatching.updateservice.add.{msb_subservices, select_activities}

import scala.concurrent.Future


@Singleton
class SubServicesController @Inject()(
                                       val authConnector: AuthConnector,
                                       implicit val dataCacheConnector: DataCacheConnector,
                                       val statusService: StatusService,
                                       val businessMatchingService: BusinessMatchingService,
                                       val helper: UpdateServiceHelper,
                                       val router: Router[AddServiceFlowModel]
                                     ) extends BaseController {


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key)) orElse OptionT.some(AddServiceFlowModel())
        } yield {
          val allSubServices: Set[MsbService] = MsbServices.all
          val noSbServices: Set[MsbService] = Set()
          val flowSubServices: Set[MsbService] = model.msbServices.getOrElse(MsbServices(noSbServices)).msbServices
          val remainingSubservices: Set[MsbService] = allSubServices diff flowSubServices

          val stuff = MsbServices(remainingSubservices)
          val form: Form2[MsbServices] = Form2(stuff)

          Ok(msb_subservices(form, edit))
        }) getOrElse InternalServerError("Failed to get activities")
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>
        Form2[MsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.updateservice.add.msb_subservices(f, edit)))

          case ValidForm(_, data) => {
            dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) {
              case Some(model) => model.msbServices(data)
            } flatMap {
              case Some(model) => router.getRoute(SubServicesPageId, model, edit)
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
          }
        }
  }
}
