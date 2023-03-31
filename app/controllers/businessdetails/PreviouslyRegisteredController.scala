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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessdetails.PreviouslyRegisteredFormProvider
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails.PreviouslyRegisteredView

import scala.concurrent.Future

class PreviouslyRegisteredController @Inject () (
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  val cc: MessagesControllerComponents,
                                                  formProvider: PreviouslyRegisteredFormProvider,
                                                  previously_registered: PreviouslyRegisteredView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
        response =>
          val form: Form[PreviouslyRegistered] = (for {
            businessDetails <- response
            prevRegistered <- businessDetails.previouslyRegistered
            fp = formProvider()
          } yield fp.fill(prevRegistered)).getOrElse(formProvider())
          Ok(previously_registered(form, edit))
      }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request => {

      formProvider().bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(previously_registered(formWithErrors, edit))),
        data =>
          dataCacheConnector.fetchAll(request.credId) flatMap {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                saved <- Option(dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key,
                  getUpdatedModel(businessType, cache.getEntry[BusinessDetails](BusinessDetails.key), data))
                )
              } yield {
                saved.map(_ => getRouting(edit))
              }).getOrElse(Future.successful(Redirect(routes.ConfirmRegisteredOfficeController.get(edit))))
          }
      )
    }
  }

  private def getUpdatedModel(businessType: BusinessType, businessDetails: BusinessDetails, data: PreviouslyRegistered): BusinessDetails = {
    businessDetails.copy(previouslyRegistered = Some(data), hasChanged = true)
  }

  private def getRouting(edit: Boolean): Result = {
    if (edit) {
      Redirect(routes.SummaryController.get)
    } else {
      Redirect(routes.ActivityStartDateController.get(edit))
    }
  }
}
