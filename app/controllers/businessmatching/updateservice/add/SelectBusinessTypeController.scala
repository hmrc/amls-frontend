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
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import jto.validation.forms.UrlFormEncoded
import jto.validation.{Rule, Write}
import models.FormTypes
import models.businessmatching.{BusinessActivity, BusinessActivities => BusinessMatchingActivities}
import models.flowmanagement.{AddBusinessTypeFlowModel, SelectBusinessTypesPageId}
import models.responsiblepeople.ResponsiblePerson
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, RepeatingSection}
import views.html.businessmatching.updateservice.add.select_activities
import services.ResponsiblePeopleService.ResponsiblePeopleListHelpers

import scala.concurrent.Future

@Singleton
class SelectBusinessTypeController @Inject()(
                                            authAction: AuthAction,
                                            implicit val dataCacheConnector: DataCacheConnector,
                                            val businessMatchingService: BusinessMatchingService,
                                            val router: Router[AddBusinessTypeFlowModel]
                                          ) extends DefaultBaseController with RepeatingSection {

  implicit val activityReader: Rule[UrlFormEncoded, BusinessActivity] =
    FormTypes.businessActivityRule("error.required.bm.register.service.single") map {
      _.businessActivities.head
    }

  implicit val activityWriter = Write[BusinessActivity, UrlFormEncoded] { a =>
    Map("businessActivities[]" -> Seq(BusinessMatchingActivities.getValue(a)))
  }

  def get(edit: Boolean = false) = authAction.async {
      implicit request =>
        (for {
          //Ensure that responsible people can be populated as required
          responsiblePeople <- OptionT(dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)) orElse OptionT.none
          model <- OptionT(dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key)(model => edit match {
            case false => model.getOrElse(AddBusinessTypeFlowModel()).fitAndProperFromResponsiblePeople(responsiblePeople.exceptInactive)
            case _ => model.getOrElse(AddBusinessTypeFlowModel())
          }))
          (names, values) <- getFormData(request.credId)
        } yield {
          val form = model.activity.fold[Form2[BusinessActivity]](EmptyForm)(a => Form2(a))
          Ok(select_activities(form, edit, values, names))
        }) getOrElse InternalServerError("Get: Unable to show Select Activities page. Failed to retrieve data")
  }

  def post(edit: Boolean = false) = authAction.async {
      implicit request =>
        Form2[BusinessActivity](request.body) match {
          case f: InvalidForm => getFormData(request.credId) map {
            case (names, values) =>
              BadRequest(select_activities(f, edit, values, names))
          } getOrElse InternalServerError("Post: Invalid form on Select Activities page")

          case ValidForm(_, data) =>
            dataCacheConnector.update[AddBusinessTypeFlowModel](request.credId, AddBusinessTypeFlowModel.key) {
              model =>
                model.getOrElse(AddBusinessTypeFlowModel()) match {
                  case m if !m.activity.contains(data) =>
                    m.activity(data).isActivityAtTradingPremises(None).tradingPremisesActivities(None)
                  case m => m.activity(data)
                }
            } flatMap {
              case Some(model) => router.getRoute(request.credId, SelectBusinessTypesPageId, model, edit)
              case _ => Future.successful(InternalServerError("Post: Cannot retrieve data: SelectActivitiesController"))
            }
        }
  }

  private def getFormData(credId: String)(implicit hc: HeaderCarrier) = for {
    model <- businessMatchingService.getModel(credId)
    activities <- OptionT.fromOption[Future](model.activities) map {
      _.businessActivities
    }
  } yield {
    val allActivities = BusinessMatchingActivities.all
    val existingActivityNames = activities.toSeq.sortBy(_.getMessage()) map {
      _.getMessage()
    }
    val activityValues = (allActivities diff activities).toSeq.sortBy(_.getMessage()) map BusinessMatchingActivities.getValue

    (existingActivityNames, activityValues)
  }
}