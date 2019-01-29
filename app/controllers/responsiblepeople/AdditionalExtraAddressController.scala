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

package controllers.responsiblepeople

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import services.AutoCompleteService
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_extra_address

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class AdditionalExtraAddressController @Inject() (
                                                   override val dataCacheConnector: DataCacheConnector,
                                                   override val authConnector: AuthConnector,
                                                   auditConnector: AuditConnector,
                                                   autoCompleteService: AutoCompleteService
                                                 ) extends RepeatingSection with BaseController {


  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request =>
      getData[ResponsiblePerson](index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,Some(ResponsiblePersonAddressHistory(_,_,Some(additionalExtraAddress))),_,_,_,_,_,_,_,_,_,_,_, _)) =>
          Ok(additional_extra_address(Form2[ResponsiblePersonAddress](additionalExtraAddress), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) =>
          Ok(additional_extra_address(Form2(DefaultAddressHistory), edit, index, flow, personName.titleName, autoCompleteService.getCountries))
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext => implicit request => {
      (Form2[ResponsiblePersonAddress](request.body) match {
        case f: InvalidForm =>
          getData[ResponsiblePerson](index) map { rp =>
            BadRequest(additional_extra_address(f, edit, index, flow, ControllerHelper.rpTitleName(rp), autoCompleteService.getCountries))
          }
        case ValidForm(_, data) => {
          getData[ResponsiblePerson](index) flatMap { responsiblePerson =>
            (for {
              rp <- responsiblePerson
              addressHistory <- rp.addressHistory
              additionalExtraAddress <- addressHistory.additionalExtraAddress
            } yield {
              val additionalExtraAddressWithTime = data.copy(timeAtAddress = additionalExtraAddress.timeAtAddress)
              updateAndRedirect(additionalExtraAddressWithTime, index, edit, flow)
            }) getOrElse updateAndRedirect(data, index, edit, flow)
          }
        }
      }).recoverWith {
        case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
      }
    }
  }

  private def updateAndRedirect(data: ResponsiblePersonAddress, index: Int, edit: Boolean, flow: Option[String])
                               (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    val doUpdate = () => updateDataStrict[ResponsiblePerson](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) => a.additionalExtraAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
        }
      )
    } map { _ =>
      data.timeAtAddress match {
        case Some(_) if edit => Redirect(routes.DetailedAnswersController.get(index, flow))
        case _ => Redirect(routes.TimeAtAdditionalExtraAddressController.get(index, edit, flow))

      }
    }

    val block = for {
      rp <- OptionT(getData[ResponsiblePerson](index))
      result <- OptionT.liftF(doUpdate())
      _ <- OptionT.liftF(auditAddressChange(data.personAddress, rp, edit))
    } yield result

    block getOrElse NotFound(notFoundView)
  }

  private def auditAddressChange(newAddress: PersonAddress, model: ResponsiblePerson, edit: Boolean)
                                (implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    if (edit) {
      val oldAddress = for {
        history <- model.addressHistory
        addr <- history.additionalExtraAddress
      } yield addr

      oldAddress.fold[Future[AuditResult]](Future.successful(Success)) { addr =>
        auditConnector.sendEvent(AddressModifiedEvent(newAddress, Some(addr.personAddress)))
      }
    }
    else {
      auditConnector.sendEvent(AddressCreatedEvent(newAddress))
    }
  }
}