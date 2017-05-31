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

package controllers.responsiblepeople

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuditConnector, AMLSAuthConnector}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ThreeYearsPlus}
import models.responsiblepeople._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure, Success}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.additional_address
import scala.concurrent.Future

trait AdditionalAddressController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  val auditConnector: AuditConnector

  final val DefaultAddressHistory = ResponsiblePersonAddress(PersonAddressUK("", "", None, None, ""), None)

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_, Some(ResponsiblePersonAddressHistory(_, Some(additionalAddress), _)),_,_,_,_,_,_,_,_,_,_, _)) =>
            Ok(additional_address(Form2[ResponsiblePersonAddress](additionalAddress), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_, _)) =>
            Ok(additional_address(Form2(DefaultAddressHistory), edit, index, fromDeclaration, personName.titleName))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        (Form2[ResponsiblePersonAddress](request.body) match {
          case f: InvalidForm =>
            getData[ResponsiblePeople](index) map { rp =>
              BadRequest(additional_address(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp)))
            }
          case ValidForm(_, data) => {
            getData[ResponsiblePeople](index) flatMap { responsiblePerson =>
              (for {
                rp <- responsiblePerson
                addressHistory <- rp.addressHistory
                additionalAddress <- addressHistory.additionalAddress
              } yield {
                val additionalAddressWithTime = data.copy(timeAtAddress = additionalAddress.timeAtAddress)
                updateAndRedirect(additionalAddressWithTime, index, edit, fromDeclaration)
              }) getOrElse updateAndRedirect(data, index, edit, fromDeclaration)
            }
          }
        }).recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def updateAndRedirect(data: ResponsiblePersonAddress, index: Int, edit: Boolean, fromDeclaration: Boolean)
                               (implicit authContext: AuthContext, request: Request[AnyContent]) = {
    val doUpdate = () => updateDataStrict[ResponsiblePeople](index) { res =>
      res.addressHistory(
        res.addressHistory match {
          case Some(a) if data.timeAtAddress.contains(ThreeYearsPlus) | data.timeAtAddress.contains(OneToThreeYears) =>
            a.additionalAddress(data).removeAdditionalExtraAddress
          case Some(a) => a.additionalAddress(data)
          case _ => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
        })
    } map { _ =>
      data.timeAtAddress match {
        case Some(_) if edit => Redirect(routes.DetailedAnswersController.get(index))
        case _ => Redirect(routes.TimeAtAdditionalAddressController.get(index, edit, fromDeclaration))
      }
    }

    (for {
      rp <- OptionT(getData[ResponsiblePeople](index))
      _ <- OptionT.liftF(auditAddressChange(data.personAddress, rp, edit)) orElse OptionT.some[Future, AuditResult](Success)
      result <- OptionT.liftF(doUpdate())
    } yield result) getOrElse NotFound(notFoundView)
  }

  private def auditAddressChange(newAddress: PersonAddress, model: ResponsiblePeople, edit: Boolean)
                                (implicit hc: HeaderCarrier, request: Request[_]): Future[AuditResult] = {
    if (edit) {
      val oldAddress = for {
        history <- model.addressHistory
        addr <- history.additionalAddress
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

object AdditionalAddressController extends AdditionalAddressController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override lazy val auditConnector = AMLSAuditConnector
}
