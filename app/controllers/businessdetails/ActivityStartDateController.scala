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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.ActivityStartDateFormProvider
import models.businessdetails._
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, UnincorporatedBody}
import models.businessmatching.{BusinessMatching, BusinessType}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails.ActivityStartDateView

import scala.concurrent.Future

class ActivityStartDateController @Inject() (
  val dataCache: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: ActivityStartDateFormProvider,
  activity_start_date: ActivityStartDateView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { response =>
      val form: Form[ActivityStartDate] = (for {
        businessDetails   <- response
        activityStartDate <- businessDetails.activityStartDate
      } yield formProvider().fill(activityStartDate)).getOrElse(formProvider())
      Ok(activity_start_date(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(activity_start_date(formWithErrors, edit))),
        data =>
          {
            dataCache.fetchAll(request.credId) flatMap { maybeCache =>
              val businessMatching = for {
                cacheMap <- maybeCache
                bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
              } yield bm

              val businessType = for {
                bt <- ControllerHelper.getBusinessType(businessMatching)
              } yield bt

              val businessDetails = for {
                cacheMap <- maybeCache
                atb      <- cacheMap.getEntry[BusinessDetails](BusinessDetails.key)
              } yield atb

              for {
                _ <-
                  dataCache
                    .save[BusinessDetails](request.credId, BusinessDetails.key, businessDetails.activityStartDate(data))
              } yield getRouting(businessType, edit)

            }
          } recoverWith { case _: IndexOutOfBoundsException =>
            Future.successful(NotFound(notFoundView))
          }
      )
  }

  private def getRouting(businessType: Option[BusinessType], edit: Boolean): Result =
    (businessType, edit) match {
      case (_, true)                                                                               => Redirect(routes.SummaryController.get)
      case (Some(UnincorporatedBody) | Some(LPrLLP) | Some(LimitedCompany) | Some(Partnership), _) =>
        Redirect(routes.VATRegisteredController.get(edit))
      case (_, false)                                                                              =>
        Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
    }
}
