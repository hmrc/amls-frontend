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

package controllers.businessdetails

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms._
import models.businessdetails._
import play.api.mvc.MessagesControllerComponents
import utils.AuthAction
import views.html.businessdetails._

import scala.concurrent.Future

class ContactingYouPhoneController @Inject () (val dataCache: DataCacheConnector,
                                               val authAction: AuthAction,
                                               val ds: CommonPlayDependencies,
                                               val cc: MessagesControllerComponents,
                                               contacting_you_phone: contacting_you_phone) extends AmlsBaseController(ds, cc) {

  def updateData(contactingYou: Option[ContactingYou], data: ContactingYouPhone): ContactingYou = {
    contactingYou.fold[ContactingYou](ContactingYou())(x => x.copy(phoneNumber = Some(data.phoneNumber)))
  }

  def get(edit: Boolean = false) = authAction.async {
    implicit request =>
      for {
        businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
      } yield businessDetails match {
        case Some(BusinessDetails(_, _, _, _, Some(details), _, _, _, _, _, _, _)) if details.phoneNumber.isDefined =>
          Ok(
            contacting_you_phone(Form2[ContactingYouPhone](ContactingYouPhone(details.phoneNumber.getOrElse(""))), edit)
          )
        case _ => Ok(contacting_you_phone(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = authAction.async {
    implicit request =>
      Form2[ContactingYouPhone](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(contacting_you_phone(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessDetails <- dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key)
            _ <- dataCache.save[BusinessDetails](
              request.credId, BusinessDetails.key, businessDetails.contactingYou(
                updateData(businessDetails.contactingYou, data)
              )
            )
          } yield {
            edit match {
              case true => Redirect(routes.SummaryController.get)
              case _ => Redirect(routes.LettersAddressController.get(edit))
            }
          }
      }
  }
}
