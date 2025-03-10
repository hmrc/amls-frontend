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
import forms.businessdetails.LettersAddressFormProvider
import models.businessdetails.{BusinessDetails, LettersAddress, RegisteredOffice}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.AuthAction
import views.html.businessdetails.LettersAddressView

import scala.concurrent.Future

class LettersAddressController @Inject() (
  val dataCache: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: LettersAddressFormProvider,
  view: LettersAddressView
) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { response =>
      (for {
        atb              <- response
        registeredOffice <- atb.registeredOffice
      } yield (for {
        altCorrespondenceAddress <- atb.altCorrespondenceAddress
      } yield Ok(view(formProvider().fill(LettersAddress(!altCorrespondenceAddress)), registeredOffice, edit)))
        .getOrElse(Ok(view(formProvider(), registeredOffice, edit)))) getOrElse Redirect(
        routes.CorrespondenceAddressIsUkController.get(edit)
      )
    }

  }

  def post(edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          dataCache.fetch[BusinessDetails](request.credId, BusinessDetails.key) map { response =>
            val regOffice: Option[RegisteredOffice] = (for {
              businessDetails  <- response
              registeredOffice <- businessDetails.registeredOffice
            } yield Option[RegisteredOffice](registeredOffice)).flatten
            regOffice match {
              case Some(data) => BadRequest(view(formWithErrors, data))
              case _          => Redirect(routes.CorrespondenceAddressIsUkController.get(edit))
            }
          },
        data =>
          dataCache.fetchAll(request.credId) flatMap { optionalCache =>
            val result = for {
              cache           <- optionalCache
              businessDetails <- cache.getEntry[BusinessDetails](BusinessDetails.key)
            } yield dataCache.save[BusinessDetails](
              request.credId,
              BusinessDetails.key,
              data.lettersAddress match {
                case true  =>
                  businessDetails
                    .altCorrespondenceAddress(false)
                    .copy(correspondenceAddress = None, correspondenceAddressIsUk = None)
                case false =>
                  businessDetails.altCorrespondenceAddress(true)
              }
            ) map { _ =>
              getRouting(data.lettersAddress, edit)
            }
            result getOrElse Future.successful(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      )
  }

  private def getRouting(altCorrespondenceAddress: Boolean, edit: Boolean): Result =
    if (altCorrespondenceAddress) {
      Redirect(routes.SummaryController.get)
    } else {
      Redirect(routes.CorrespondenceAddressIsUkController.get(edit))
    }
}
