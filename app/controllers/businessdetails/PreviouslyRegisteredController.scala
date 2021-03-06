/*
 * Copyright 2021 HM Revenue & Customs
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

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessdetails._
import models.businessmatching.{BusinessMatching, BusinessType}
import play.api.mvc.{MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper}
import views.html.businessdetails._

import scala.concurrent.Future

class PreviouslyRegisteredController @Inject () (
                                                  val dataCacheConnector: DataCacheConnector,
                                                  val authAction: AuthAction,
                                                  val ds: CommonPlayDependencies,
                                                  val cc: MessagesControllerComponents,
                                                  previously_registered: previously_registered) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetch[BusinessDetails](request.credId, BusinessDetails.key) map {
        response =>
          val form: Form2[PreviouslyRegistered] = (for {
            businessDetails <- response
            prevRegistered <- businessDetails.previouslyRegistered
          } yield Form2[PreviouslyRegistered](prevRegistered)).getOrElse(EmptyForm)
          Ok(previously_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request => {
      Form2[PreviouslyRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(previously_registered(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll(request.credId) flatMap {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                saved <- Option(dataCacheConnector.save[BusinessDetails](request.credId, BusinessDetails.key,
                  getUpdatedModel(businessType,  cache.getEntry[BusinessDetails](BusinessDetails.key), data))
                )
              } yield {
                saved.map(_ => getRouting(businessType, edit, data))
              }).getOrElse(Future.successful(Redirect(routes.ConfirmRegisteredOfficeController.get(edit))))
          }
      }
    }
  }

  private def getUpdatedModel(businessType: BusinessType, businessDetails: BusinessDetails, data: PreviouslyRegistered): BusinessDetails = {
    businessDetails.copy(previouslyRegistered = Some(data), hasChanged = true)
  }

  private def getRouting(businessType: BusinessType, edit: Boolean, data: PreviouslyRegistered): Result = {
    (edit) match {
      case true => Redirect(routes.SummaryController.get())
      case _    => Redirect (routes.ActivityStartDateController.get(edit))
    }
  }
}
