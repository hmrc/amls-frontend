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
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness._
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait ContactingYouController extends BaseController {

  val dataCache: DataCacheConnector

  def updateData(contactingYou: Option[ContactingYou], data: ContactingYouEmail): Option[ContactingYou] = {
    contactingYou.fold[Option[ContactingYou]](Some(ContactingYou()))(x => Some(x.copy(email = Some(data.email))))
  }

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        aboutTheBusiness <-
        dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
      } yield aboutTheBusiness match {
        case Some(AboutTheBusiness(_,_, _, _, Some(details), _, _, _)) =>
          Ok(contacting_you(Form2[ContactingYou](details), edit))
        case Some(AboutTheBusiness(_,_, _, _, None, _, _, _)) =>
          Ok(contacting_you(EmptyForm, edit))
        case _ =>
          // TODO: Make sure this redirects to the right place
          Redirect(routes.ConfirmRegisteredOfficeController.get(edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ContactingYouEmail](request.body) match {
        case f: InvalidForm =>
              Future.successful(BadRequest(contacting_you(f, edit)))
        case ValidForm(_, data) =>
          if (!data.email.equals(data.confirmEmail)) {
            val in = InvalidForm(Map("email" -> Seq(data.email), "confirmEmail" -> Seq(data.confirmEmail)),
              List(( Path \ "",List(ValidationError(List("error.mismatch.atb.email"))))))
            Future.successful(BadRequest(contacting_you(in, edit)))
          }else{
            for {
              aboutTheBusiness <- dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key)
              _ <- dataCache.save[AboutTheBusiness](AboutTheBusiness.key,
                aboutTheBusiness.copy(contactingYou = updateData(aboutTheBusiness.contactingYou, data))
              )
            } yield {
              edit match {
                case true => Redirect(routes.SummaryController.get())
                case _ => Redirect(routes.ContactingYouPhoneController.get(edit))
              }
            }
          }
      }
  }
}

object ContactingYouController extends ContactingYouController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCache = DataCacheConnector
}
