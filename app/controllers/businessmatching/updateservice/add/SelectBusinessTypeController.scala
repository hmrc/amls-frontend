/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Rule, Write}
import models.FormTypes
import models.businessmatching.{BusinessActivity, BusinessActivities => BusinessMatchingActivities}
import models.flowmanagement.{AddBusinessTypeFlowModel, SelectBusinessTypesPageId}
import play.api.i18n.Messages
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.add.select_activities

import scala.concurrent.Future

@Singleton
class SelectBusinessTypeController @Inject()(
                                            authAction: AuthAction,
                                            val ds: CommonPlayDependencies,
                                            implicit val dataCacheConnector: DataCacheConnector,
                                            val businessMatchingService: BusinessMatchingService,
                                            val router: Router[AddBusinessTypeFlowModel],
                                            val addHelper: AddBusinessTypeHelper,
                                            val cc: MessagesControllerComponents,
                                            select_activities: select_activities) extends AmlsBaseController(ds, cc) with RepeatingSection {

  implicit val activityReader: Rule[UrlFormEncoded, BusinessActivity] =
    FormTypes.businessActivityRule("error.required.bm.register.service") map {
      _.businessActivities.head
    }

  implicit val activityWriter = Write[BusinessActivity, UrlFormEncoded] { a =>
    Map("businessActivities[]" -> Seq(BusinessMatchingActivities.getValue(a)))
  }

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          model <- OptionT(dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)(model =>
           model.getOrElse(AddBusinessTypeFlowModel())))
          (names, values) <- getFormData(request.credId)
        } yield {
          val form = model.activity.fold[Form2[BusinessActivity]](EmptyForm)(a => Form2(a))
          Ok(select_activities(form, edit, values, names.toSeq))
        }) getOrElse InternalServerError("Get: Unable to show Select Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[BusinessActivity](request.body) match {
          case f: InvalidForm => getFormData(request.credId) map {
            case (names, values) =>
              BadRequest(select_activities(f, edit, values, names.toSeq))
          } getOrElse InternalServerError("Post: Invalid form on Select Activities page")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              model =>
                model.getOrElse(AddBusinessTypeFlowModel()).activity(data)
            } flatMap {
              case Some(model) => router.getRoute(request.credId, SelectBusinessTypesPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: SelectActivitiesController"))
            }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier, messages: Messages) = for {
    model <- businessMatchingService.getModel(credId)
    activities <- OptionT.fromOption[Future](model.activities) map {
      _.businessActivities
    }
  } yield {
    val allActivities = BusinessMatchingActivities.all
    val existingActivityNames = addHelper.prefixedActivities(model)
    val activityValues = (allActivities diff activities).toSeq.sortBy(_.getMessage(true)) map BusinessMatchingActivities.getValue

    (existingActivityNames, activityValues)
  }
}