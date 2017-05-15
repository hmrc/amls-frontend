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

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness._
import views.html.aboutthebusiness._

trait ContactingYouController extends BaseController {

  val dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_,_, _, _, Some(details), Some(registeredOffice), _, _)) =>
          Ok(contacting_you(Form2[ContactingYou](details), registeredOffice, edit))
        case Some(AboutTheBusiness(_,_, _, _, None, Some(registeredOffice), _, _)) =>
          Ok(contacting_you(EmptyForm, registeredOffice, edit))
        case _ =>
          // TODO: Make sure this redirects to the right place
          Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ContactingYouForm](request.body) match {
        case f: InvalidForm =>
          for {
            aboutTheBusiness <-
            dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
          } yield aboutTheBusiness match {
            case Some(AboutTheBusiness(_, _,_, _, _, Some(registeredOffice), _, _)) =>
              BadRequest(contacting_you(f, registeredOffice, edit))
            case _ =>
              Redirect(routes.ContactingYouController.get(edit))
          }
        case ValidForm(_, data) =>
          for {
            aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
            _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
              aboutTheBusiness.contactingYou(data).correspondenceAddress(None)
            )
          } yield data.letterToThisAddress match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.CorrespondenceAddressController.get(edit))
          }
      }
  }
}

object ContactingYouController extends ContactingYouController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCache = DataCacheConnector
}
