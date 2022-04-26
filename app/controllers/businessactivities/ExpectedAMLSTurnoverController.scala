/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.businessactivities

import cats.data.OptionT
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover}
import models.businessmatching._
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.businessactivities._
import cats.implicits._

import scala.concurrent.Future

class ExpectedAMLSTurnoverController @Inject() (val dataCacheConnector: DataCacheConnector,
                                                val authAction: AuthAction,
                                                val ds: CommonPlayDependencies,
                                                implicit val statusService: StatusService,
                                                val cc: MessagesControllerComponents,
                                                expected_amls_turnover: expected_amls_turnover) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId) map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            (for {
              businessActivities <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              expectedTurnover <- businessActivities.expectedAMLSTurnover
            } yield Ok(expected_amls_turnover(
              Form2[ExpectedAMLSTurnover](expectedTurnover),
              edit,
              businessMatching,
              businessMatching.alphabeticalBusinessActivitiesLowerCase()
            ))).getOrElse (Ok(expected_amls_turnover(
              EmptyForm,
              edit,
              businessMatching,
              businessMatching.alphabeticalBusinessActivitiesLowerCase()
            )))
          }) getOrElse Ok(expected_amls_turnover(EmptyForm, edit, None, None))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      getErrorMessage(request.credId) flatMap { errorMsg =>
      Form2[ExpectedAMLSTurnover](request.body)(ExpectedAMLSTurnover.formRuleWithErrorMsg(errorMsg)) match {
        case f: InvalidForm =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield {
            BadRequest(expected_amls_turnover(f, edit, businessMatching, businessMatching.alphabeticalBusinessActivitiesLowerCase()))
          }

        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get)
            case false => Redirect(routes.BusinessFranchiseController.get())
          }
      }
      }
    }
  private def getErrorMessage(credId: String)(implicit hc: HeaderCarrier) = {
    (for {
      businessMatching <- OptionT(dataCacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key))
      activities <- OptionT.fromOption[Future](businessMatching.activities)
    } yield {
      if (activities.businessActivities.size == 1) {
        "error.required.ba.turnover.from.mlr.single"
      } else {
        "error.required.ba.turnover.from.mlr"
      }
    }) getOrElse "error.required.ba.turnover.from.mlr"
  }
}
