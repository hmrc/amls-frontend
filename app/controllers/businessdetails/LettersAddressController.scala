/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.BaseController
import models.businessdetails.{BusinessDetails, LettersAddress, RegisteredOffice}
import views.html.businessdetails._
import play.api.mvc.Result
import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import com.google.inject.Inject
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future


class LettersAddressController @Inject () (
                                          val dataCache: DataCacheConnector,
                                          val authConnector: AuthConnector
                                          )extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[BusinessDetails](BusinessDetails.key) map {
        response =>
          (for {
            atb <- response
            registeredOffice <- atb.registeredOffice
          } yield {
            (for {
              altCorrespondenceAddress <- atb.altCorrespondenceAddress
            } yield Ok(letters_address(Form2[LettersAddress](LettersAddress(!altCorrespondenceAddress)), registeredOffice, edit)))
              .getOrElse (Ok(letters_address(EmptyForm, registeredOffice, edit)))
          }) getOrElse Redirect(routes.CorrespondenceAddressIsUkController.get(edit))
      }

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[LettersAddress](request.body) match {
        case f: InvalidForm =>
          dataCache.fetch[BusinessDetails](BusinessDetails.key) map {
            response =>
              val regOffice: Option[RegisteredOffice] = (for {
                businessDetails <- response
                registeredOffice <- businessDetails.registeredOffice
              } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
              regOffice match {
                case Some(data) => BadRequest(letters_address(f, data))
                case _ => Redirect(routes.CorrespondenceAddressIsUkController.get(edit))
              }
          }
        case ValidForm(_, data) =>
          dataCache.fetchAll flatMap {
            optionalCache =>
              val result = for {
                cache <- optionalCache
                businessDetails <- cache.getEntry[BusinessDetails](BusinessDetails.key)
              } yield {
                dataCache.save[BusinessDetails](BusinessDetails.key, data.lettersAddress match {
                  case true =>
                    businessDetails.altCorrespondenceAddress(false).copy(correspondenceAddress = None, correspondenceAddressIsUk = None)
                  case false =>
                    businessDetails.altCorrespondenceAddress(true)
                }) map {
                  _ => getRouting(data.lettersAddress, edit)
                }
              }
              result getOrElse Future.successful(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
  }

  private def getRouting(altCorrespondenceAddress: Boolean, edit: Boolean): Result = {
    altCorrespondenceAddress match {
      case true => Redirect(routes.SummaryController.get())
      case false => Redirect(routes.CorrespondenceAddressIsUkController.get(edit))
    }
  }
}