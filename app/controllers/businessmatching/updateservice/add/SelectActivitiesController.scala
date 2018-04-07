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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Rule, Write}
import models.FormTypes
import models.businessmatching.{BusinessActivity, MoneyServiceBusiness, TrustAndCompanyServices, BusinessActivities => BusinessMatchingActivities}
import models.flowmanagement.{AddServiceFlowModel, SelectActivitiesPageId}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.routings.VariationAddServiceRouter.router
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection
import views.html.businessmatching.updateservice.select_activities


import scala.concurrent.Future

@Singleton
class SelectActivitiesController @Inject()(val authConnector: AuthConnector,
                                           val statusService: StatusService,
                                           implicit val dataCacheConnector: DataCacheConnector,
                                           val businessMatchingService: BusinessMatchingService) extends BaseController with RepeatingSection {

  implicit val activityReader: Rule[UrlFormEncoded, BusinessActivity] =
    FormTypes.businessActivityRule("error.required.bm.register.service.single") map { _.businessActivities.head }

  implicit val activityWriter = Write[BusinessActivity, UrlFormEncoded] { a =>
    Map("businessActivities[]" -> Seq(BusinessMatchingActivities.getValue(a)))
  }

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.fetch[AddServiceFlowModel](AddServiceFlowModel.key)) orElse OptionT.some(AddServiceFlowModel())
          (names, values) <- getFormData
        } yield {
          val form = model.activity.fold[Form2[BusinessActivity]](EmptyForm)(a => Form2(a))

          Ok(select_activities(form, edit, values, names))
        }) getOrElse InternalServerError("Failed to get activities")
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[BusinessActivity](request.body) match {
          case f: InvalidForm => getFormData map {
            case (names, values) =>
              BadRequest(select_activities(f, edit, values, names))
          } getOrElse InternalServerError("Could not get form data")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) { model =>
              model.getOrElse(AddServiceFlowModel()) match {
                case m if !m.activity.contains(data) =>
                    m.activity(data).isActivityAtTradingPremises(None).tradingPremisesActivities(None)
                case m => m.activity(data)
              }
            } flatMap { case Some(model) =>
              router.getRoute(SelectActivitiesPageId, model, edit)
            }
        }
  }

  private def getFormData(implicit ac: AuthContext, hc: HeaderCarrier) = for {
    existing <- businessMatchingService.getSubmittedBusinessActivities
    } yield {
      val availableActivities = BusinessMatchingActivities.all.filterNot {
        case MoneyServiceBusiness | TrustAndCompanyServices => true
        case _ => false
      }

      val existingActivityNames = existing map { _.getMessage }
      val activityValues = (availableActivities diff existing) map BusinessMatchingActivities.getValue

      (existingActivityNames, activityValues)
    }

}