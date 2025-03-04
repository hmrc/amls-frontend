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

package controllers.hvd

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import models.DateOfChange
import models.businessdetails.{ActivityStartDate, BusinessDetails}
import models.hvd.Hvd
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import utils.{AuthAction, DateHelper, DateOfChangeHelper, RepeatingSection}
import views.html.DateOfChangeView

import javax.inject.Inject
import scala.concurrent.Future

class HvdDateOfChangeController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: DateOfChangeFormProvider,
  view: DateOfChangeView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper {

  def get(redirect: String): Action[AnyContent] = authAction { implicit request =>
    Ok(getView(formProvider(), redirect))
  }

  def compareAndUpdateDate(hvd: Hvd, newDate: DateOfChange): Hvd =
    hvd.dateOfChange match {
      case Some(s4ltrDate) =>
        s4ltrDate.dateOfChange.isBefore(newDate.dateOfChange) match {
          case true  => hvd
          case false => hvd.dateOfChange(newDate)
        }
      case _               => hvd.dateOfChange(newDate)
    }

  def post(redirect: String): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getView(formWithErrors, redirect))),
        dateOfChange =>
          getModelWithDateMap(request.credId).flatMap {
            case (hvd, Some(activityStartDate)) if !dateOfChange.dateOfChange.isBefore(activityStartDate.startDate) =>
              dataCacheConnector.save[Hvd](request.credId, Hvd.key, compareAndUpdateDate(hvd, dateOfChange)) map { _ =>
                Redirect(DateOfChangeRedirect(redirect).call)
              }
            case (_, Some(activityStartDate))                                                                       =>
              Future.successful(
                BadRequest(
                  getView(
                    formProvider().withError(
                      "dateOfChange",
                      messages(
                        "error.expected.dateofchange.date.after.activitystartdate",
                        DateHelper.formatDate(activityStartDate.startDate)
                      )
                    ),
                    redirect
                  )
                )
              )
            case (_, None)                                                                                          =>
              Future.failed(new Exception("Could not retrieve start date"))
          }
      )
  }

  private def getModelWithDateMap(credId: String): Future[(Hvd, Option[ActivityStartDate])] =
    dataCacheConnector.fetchAll(credId) map { optionalCache =>
      (for {
        cache           <- optionalCache
        businessDetails <- cache.getEntry[BusinessDetails](BusinessDetails.key)
        hvd             <- cache.getEntry[Hvd](Hvd.key)
      } yield (hvd, businessDetails.activityStartDate)) match {
        case Some((hvd, Some(activityStartDate))) => (hvd, Some(activityStartDate))
        case Some((hvd, _))                       => (hvd, None)
        case _                                    => (Hvd(), None)
      }
    }

  private def getView(form: Form[DateOfChange], redirect: String)(implicit request: Request[_]): Html = view(
    form,
    "summary.hvd",
    routes.HvdDateOfChangeController.post(redirect)
  )
}
