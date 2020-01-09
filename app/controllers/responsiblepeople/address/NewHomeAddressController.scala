/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.responsiblepeople.address

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.{Inject, Singleton}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthAction, ControllerHelper}
import views.html.responsiblepeople.address.new_home_address

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class NewHomeAddressController @Inject()(authAction: AuthAction,
                                         val dataCacheConnector: DataCacheConnector,
                                         val ds: CommonPlayDependencies,
                                         val cc: MessagesControllerComponents) extends AmlsBaseController(ds, cc) with AddressHelper {

  def get(index: Int) = authAction.async {
    implicit request =>
      for {
        rp <- getData[ResponsiblePerson](request.credId, index)
        newAddress <- dataCacheConnector.fetch[NewHomeAddress](request.credId, NewHomeAddress.key)
      } yield (rp, newAddress) match {
        case (Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)), Some(newHomeAddress))
        => Ok(new_home_address(Form2[NewHomeAddress](newHomeAddress), index, personName.titleName))
        case (Some(ResponsiblePerson(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _)), None)
        => Ok(new_home_address(EmptyForm, index, personName.titleName))
        case _
        => NotFound(notFoundView)
      }
  }

  def post(index: Int) =
    authAction.async {
      implicit request =>
        (Form2[NewHomeAddress](request.body)(NewHomeAddress.addressFormRule(PersonAddress.formRule(AddressType.NewHome))) match {
          case f: InvalidForm if f.data.get("isUK").isDefined
          => processFormAndRedirect(NewHomeAddress(modelFromForm(f)), index, request.credId)
          case f: InvalidForm
          => getData[ResponsiblePerson](request.credId, index) map { rp =>
            BadRequest(new_home_address(f, index, ControllerHelper.rpTitleName(rp)))
          }
          case ValidForm(_, data)
          => processFormAndRedirect(data, index, request.credId)
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
    }

  def processFormAndRedirect(data: NewHomeAddress, index: Int, credId: String)
                            (implicit request: Request[AnyContent], hc: HeaderCarrier) = {
    for {
      redirect <- dataCacheConnector.save[NewHomeAddress](credId, NewHomeAddress.key, data) map { _ =>
        data.personAddress match {
          case _: PersonAddressUK => Redirect(routes.NewHomeAddressUKController.get(index))
          case _: PersonAddressNonUK => Redirect(routes.NewHomeAddressNonUKController.get(index))
          case _ => NotFound(notFoundView)
        }
      }
    } yield redirect
  }
}
