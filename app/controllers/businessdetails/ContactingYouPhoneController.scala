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
import forms.businessdetails.BusinessTelephoneFormProvider
import models.businessdetails._
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.AuthAction
import views.html.businessdetails.BusinessTelephoneView

import scala.concurrent.Future

class ContactingYouPhoneController @Inject() (
  val dataCache: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: BusinessTelephoneFormProvider,
  view: BusinessTelephoneView
) extends AmlsBaseController(ds, cc) {

  def updateData(contactingYou: Option[ContactingYou], data: ContactingYouPhone): ContactingYou =
    contactingYou.fold[ContactingYou](ContactingYou(Some(data.phoneNumber))) {
      _.copy(phoneNumber = Some(data.phoneNumber))
    }

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    implicit val form: Form[ContactingYouPhone] = formProvider()
    for {
      businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
    } yield businessDetails match {
      case Some(BusinessDetails(_, _, _, _, Some(details), _, _, _, _, _, _, _)) if details.phoneNumber.isDefined =>
        Ok(
          view(details.phoneNumber.fold(form)(x => form.fill(ContactingYouPhone(x))), edit)
        )
      case _                                                                                                      => Ok(view(form, edit))
    }
  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(view(formWithError, edit))),
        data =>
          for {
            businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
            _               <- dataCache.save[BusinessDetails](
                                 request.credId,
                                 BusinessDetails.key,
                                 businessDetails.contactingYou(
                                   updateData(businessDetails.contactingYou, data)
                                 )
                               )
          } yield
            if (edit) {
              Redirect(routes.SummaryController.get)
            } else {
              Redirect(routes.LettersAddressController.get(edit))
            }
      )
  }
}
