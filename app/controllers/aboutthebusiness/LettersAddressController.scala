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
import models.aboutthebusiness.{AboutTheBusiness, LettersAddress, RegisteredOffice}
import views.html.aboutthebusiness._
import play.api.mvc.Result
import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}


trait LettersAddressController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          (for {
            atb <- response
            registeredOffice <- atb.registeredOffice
          } yield {
            (for {
              altCorrespondenceAddress <- atb.altCorrespondenceAddress
            } yield Ok(letters_address(Form2[LettersAddress](LettersAddress(!altCorrespondenceAddress)), registeredOffice, edit)))
              .getOrElse (Ok(letters_address(EmptyForm, registeredOffice, edit)))
          }) getOrElse Redirect(routes.CorrespondenceAddressController.get(edit))
      }

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[LettersAddress](request.body) match {
        case f: InvalidForm =>
          dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
            response =>
              val regOffice: Option[RegisteredOffice] = (for {
                aboutTheBusiness <- response
                registeredOffice <- aboutTheBusiness.registeredOffice
              } yield Option[RegisteredOffice](registeredOffice)).getOrElse(None)
              regOffice match {
                case Some(data) => BadRequest(letters_address(f, data))
                case _ => Redirect(routes.CorrespondenceAddressController.get(edit))
              }
          }
        case ValidForm(_, data) =>
          dataCache.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCache.save[AboutTheBusiness](AboutTheBusiness.key, data.lettersAddress match {
                  case true =>
                    aboutTheBusiness.altCorrespondenceAddress(false).copy(correspondenceAddress = None)
                  case false =>
                    aboutTheBusiness.altCorrespondenceAddress(true)
                })

                getRouting(data.lettersAddress, edit)
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
  }

  private def getRouting(altCorrespondenceAddress: Boolean, edit: Boolean): Result = {
    altCorrespondenceAddress match {
      case true => Redirect(routes.SummaryController.get())
      case false => Redirect(routes.CorrespondenceAddressController.get(edit))
    }
  }
}

object LettersAddressController extends LettersAddressController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
