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

package controllers.renewal

import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.renewal.AMLSTurnoverFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.renewal.AMLSTurnover
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RenewalService
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.AMLSTurnoverView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AMLSTurnoverController @Inject() (
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val cc: MessagesControllerComponents,
  formProvider: AMLSTurnoverFormProvider,
  view: AMLSTurnoverView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    val futureFormFillBusinessActivities: Future[Form[AMLSTurnover]] =
      renewalService.getFirstBusinessActivityInLowercase(request.credId).map(formProvider(_))
    val futureAMLSTurnover: Future[Option[AMLSTurnover]]             = renewalService.getRenewal(request.credId).map(_.turnover)

    for {
      optBm       <- renewalService.getBusinessMatching(request.credId)
      form        <- futureFormFillBusinessActivities
      optTurnover <- futureAMLSTurnover
    } yield {
      val filledFormOrEmpty = optTurnover.map(turnover => form.fill(turnover)).getOrElse(form)
      Ok(view(filledFormOrEmpty, edit, optBm.flatMap(_.alphabeticalBusinessActivitiesLowerCase())))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    renewalService.getFirstBusinessActivityInLowercase(request.credId) flatMap {
      formProvider(_)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            renewalService.getBusinessMatching(request.credId) map { bm =>
              BadRequest(view(formWithErrors, edit, bm.alphabeticalBusinessActivitiesLowerCase()))
            },
          data =>
            for {
              renewal          <- renewalService.getRenewal(request.credId)
              _                <- renewalService.updateRenewal(request.credId, renewal.turnover(data))
              businessMatching <- renewalService.getBusinessMatching(request.credId)
            } yield
              if (edit) {
                Redirect(routes.SummaryController.get)
              } else {
                getRouting(ControllerHelper.getBusinessActivity(businessMatching))
              }
        )
    }
  }

  private def getRouting(ba: Option[BusinessActivities]) =
    ba match {
      case Some(activities) =>
        activities.businessActivities match {
          case x if x.contains(ArtMarketParticipant) => Redirect(routes.AMPTurnoverController.get())
          case x if x.contains(MoneyServiceBusiness) => Redirect(routes.TotalThroughputController.get())
          case x if x.contains(AccountancyServices)  => Redirect(routes.CustomersOutsideIsUKController.get())
          case x if x.contains(HighValueDealing)     => Redirect(routes.CustomersOutsideIsUKController.get())
          case _                                     => Redirect(routes.SummaryController.get)
        }
      case _                => InternalServerError("Unable to redirect from Turnover page")
    }

}
