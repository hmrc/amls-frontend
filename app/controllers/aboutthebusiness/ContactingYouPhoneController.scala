/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.aboutthebusiness

import java.lang.ProcessBuilder.Redirect

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness._
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait ContactingYouPhoneController extends BaseController {

  val dataCache: DataCacheConnector

  def updateData(contactingYou: Option[ContactingYou], data: ContactingYouPhone): ContactingYou = {
    contactingYou.fold[ContactingYou](ContactingYou())(x => x.copy(phoneNumber = Some(data.phoneNumber)))
  }

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_,_, _, _, Some(details), _, _, _)) if details.phoneNumber.isDefined =>
          Ok(contacting_you_phone(Form2[ContactingYouPhone](ContactingYouPhone (details.phoneNumber.getOrElse(""))), edit))
        case _ => Ok(contacting_you_phone(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ContactingYouPhone](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(contacting_you_phone(f, edit)))
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
                aboutTheBusiness.contactingYou(updateData(aboutTheBusiness.contactingYou, data))
            )
          } yield {
            edit match {
              case true => Redirect(routes.SummaryController.get())
              case _ => Redirect(routes.LettersAddressController.get(edit))
            }
          }
      }
  }
}

object ContactingYouPhoneController extends ContactingYouPhoneController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCache = DataCacheConnector
}
