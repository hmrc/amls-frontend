/*
 * Copyright 2023 HM Revenue & Customs
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
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessactivities.ExpectedAMLSTurnoverFormProvider
import models.businessactivities.BusinessActivities
import models.businessmatching._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthAction
import views.html.businessactivities.ExpectedAMLSTurnoverView

import scala.concurrent.Future

class ExpectedAMLSTurnoverController @Inject()(val dataCacheConnector: DataCacheConnector,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               implicit val statusService: StatusService,
                                               val cc: MessagesControllerComponents,
                                               formProvider: ExpectedAMLSTurnoverFormProvider,
                                               view: ExpectedAMLSTurnoverView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
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
            } yield Ok(view(
              formProvider().fill(expectedTurnover),
              edit,
              businessMatching,
              businessMatching.alphabeticalBusinessActivitiesLowerCase()
            ))).getOrElse (Ok(view(
              formProvider(),
              edit,
              businessMatching,
              businessMatching.alphabeticalBusinessActivitiesLowerCase()
            )))
          }) getOrElse Ok(view(formProvider(), edit, None, None))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      getErrorMessage(request.credId) flatMap { errorMsg =>
      formProvider().bindFromRequest().fold(
        formWithErrors =>
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield {
            BadRequest(view(formWithErrors, edit, businessMatching, businessMatching.alphabeticalBusinessActivitiesLowerCase()))
          },
        data =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](request.credId, BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](request.credId, BusinessActivities.key,
              businessActivities.expectedAMLSTurnover(data)
            )
          } yield if (edit) {
            Redirect(routes.SummaryController.get)
          } else {
            Redirect(routes.BusinessFranchiseController.get())
          }
        )
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
