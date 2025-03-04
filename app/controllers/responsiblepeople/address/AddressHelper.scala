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

package controllers.responsiblepeople.address

import audit.AddressConversions._
import audit.{AddressCreatedEvent, AddressModifiedEvent}
import cats.data.OptionT
import cats.implicits._
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ThreeYearsPlus, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.SubmissionStatus
import models.{Country, DateOfChange, ViewResponse}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.{ControllerHelper, DateOfChangeHelper, RepeatingSection}

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

trait AddressHelper extends RepeatingSection with DateOfChangeHelper {

  private[address] def updateAdditionalAddressAndRedirect(
    credId: String,
    data: ResponsiblePersonAddress,
    index: Int,
    edit: Boolean,
    flow: Option[String]
  )(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    auditConnector: AuditConnector,
    messages: Messages,
    error: views.html.ErrorView
  ): Future[Result] = {

    import play.api.mvc.Results._

    val doUpdate = () =>
      updateDataStrict[ResponsiblePerson](credId, index) { res =>
        res.addressHistory(res.addressHistory match {
          case Some(a) if data.timeAtAddress.contains(ThreeYearsPlus) | data.timeAtAddress.contains(OneToThreeYears) =>
            a.additionalAddress(data).removeAdditionalExtraAddress
          case Some(a)                                                                                               => a.additionalAddress(data)
          case _                                                                                                     => ResponsiblePersonAddressHistory(additionalAddress = Some(data))
        })
      } map { _ =>
        data.timeAtAddress match {
          case Some(_) if edit =>
            Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
          case _               => Redirect(routes.TimeAtAdditionalAddressController.get(index, edit, flow))
        }
      }

    (for {
      rp     <- OptionT(getData[ResponsiblePerson](credId, index))
      _      <- OptionT.liftF(auditPreviousAddressChange(data.personAddress, rp, edit)) orElse OptionT.some(Success)
      result <- OptionT.liftF(doUpdate())
    } yield result) getOrElse NotFound(ControllerHelper.notFoundView(request, messages, error))
  }

  private[address] def updateAdditionalExtraAddressAndRedirect(
    credId: String,
    data: ResponsiblePersonAddress,
    index: Int,
    edit: Boolean,
    flow: Option[String]
  )(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    auditConnector: AuditConnector,
    messages: Messages,
    error: views.html.ErrorView
  ): Future[Result] = {

    import play.api.mvc.Results._

    val doUpdate = () =>
      updateDataStrict[ResponsiblePerson](credId, index) { res =>
        res.addressHistory(
          res.addressHistory match {
            case Some(a) => a.additionalExtraAddress(data)
            case _       => ResponsiblePersonAddressHistory(additionalExtraAddress = Some(data))
          }
        )
      } map { _ =>
        data.timeAtAddress match {
          case Some(_) if edit =>
            Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
          case _               => Redirect(routes.TimeAtAdditionalExtraAddressController.get(index, edit, flow))
        }
      }

    (for {
      rp     <- OptionT(getData[ResponsiblePerson](credId, index))
      result <- OptionT.liftF(doUpdate())
      _      <- OptionT.liftF(auditPreviousExtraAddressChange(data.personAddress, rp, edit))
    } yield result).getOrElse(NotFound(ControllerHelper.notFoundView(request, messages, error)))
  }

  private[address] def updateCurrentAddressAndRedirect(
    credId: String,
    data: ResponsiblePersonCurrentAddress,
    index: Int,
    edit: Boolean,
    flow: Option[String],
    originalResponsiblePerson: Option[ResponsiblePerson],
    status: SubmissionStatus
  )(implicit
    request: Request[AnyContent],
    hc: HeaderCarrier,
    ec: ExecutionContext,
    auditConnector: AuditConnector
  ): Future[Result] = {
    import play.api.mvc.Results._

    updateDataStrict[ResponsiblePerson](credId, index) { res =>
      res.addressHistory(res.addressHistory match {
        case Some(a) => a.currentAddress(data)
        case _       => ResponsiblePersonAddressHistory(currentAddress = Some(data))
      })
    } flatMap { _ =>
      val oldAddress = for {
        viewResponse  <- OptionT(dataCacheConnector.fetch[ViewResponse](credId, ViewResponse.key))
        rp            <- OptionT.fromOption[Future](
                           ResponsiblePerson.getResponsiblePersonFromData(viewResponse.responsiblePeopleSection, index)
                         )
        address       <- OptionT.fromOption[Future](rp.addressHistory)
        personAddress <- OptionT.fromOption[Future](address.currentAddress)
      } yield personAddress.personAddress

      oldAddress.value flatMap { originalAddress =>
        (edit, originalAddress) match {
          case (true, _) =>
            auditConnector.sendEvent(AddressModifiedEvent(data.personAddress, originalAddress)) map { _ =>
              if (
                redirectToDateOfChange[PersonAddress](status, originalAddress, data.personAddress)
                && originalResponsiblePerson.flatMap { orp =>
                  orp.lineId
                }.isDefined
              ) {
                Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit))
              } else {
                Redirect(controllers.responsiblepeople.routes.DetailedAnswersController.get(index, flow))
              }
            }
          case (false, Some(a))
              if !data.personAddress.equals(a) & (a.isEmpty | a.isComplete)
                & isEligibleForDateOfChange(status) & originalResponsiblePerson.flatMap { orp =>
                  orp.lineId
                }.isDefined =>
            auditConnector.sendEvent(AddressModifiedEvent(data.personAddress, originalAddress)) map { _ =>
              Redirect(routes.CurrentAddressDateOfChangeController.get(index, edit))
            }
          case (_, _)    =>
            auditConnector.sendEvent(AddressCreatedEvent(data.personAddress)) map { _ =>
              Redirect(routes.TimeAtCurrentAddressController.get(index, edit, flow))
            }
        }
      }
    }
  }

  def modelFromPlayForm(f: Form[_]): PersonAddress =
    if (f.data.contains("isUK")) {
      PersonAddressUK("", None, None, None, "")
    } else {
      PersonAddressNonUK("", None, None, None, Country("", ""))
    }

  private[address] def auditPreviousAddressChange(newAddress: PersonAddress, model: ResponsiblePerson, edit: Boolean)(
    implicit
    hc: HeaderCarrier,
    request: Request[_],
    auditConnector: AuditConnector,
    ec: ExecutionContext
  ): Future[AuditResult] =
    if (edit) {
      val oldAddress = for {
        history <- model.addressHistory
        addr    <- history.additionalAddress
      } yield addr

      oldAddress.fold[Future[AuditResult]](Future.successful(Success)) { addr =>
        auditConnector.sendEvent(AddressModifiedEvent(newAddress, Some(addr.personAddress)))
      }
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(newAddress))
    }

  private[address] def auditPreviousExtraAddressChange(
    newAddress: PersonAddress,
    model: ResponsiblePerson,
    edit: Boolean
  )(implicit
    hc: HeaderCarrier,
    request: Request[_],
    auditConnector: AuditConnector,
    ec: ExecutionContext
  ): Future[AuditResult] =
    if (edit) {
      val oldAddress = for {
        history <- model.addressHistory
        addr    <- history.additionalExtraAddress
      } yield addr

      oldAddress.fold[Future[AuditResult]](Future.successful(Success)) { addr =>
        auditConnector.sendEvent(AddressModifiedEvent(newAddress, Some(addr.personAddress)))
      }
    } else {
      auditConnector.sendEvent(AddressCreatedEvent(newAddress))
    }

  private[address] def getTimeAtAddress(dateOfMove: Option[NewHomeDateOfChange]): Option[TimeAtAddress] =
    dateOfMove flatMap { dateOp =>
      dateOp.dateOfChange map { date =>
        ChronoUnit.MONTHS.between(date, LocalDate.now()).toInt match {
          case m if 0 until 6 contains m   => ZeroToFiveMonths
          case m if 6 until 12 contains m  => SixToElevenMonths
          case m if 12 until 36 contains m => OneToThreeYears
          case _                           => ThreeYearsPlus
        }
      }
    }

  private[address] def pushCurrentAddress(
    currentAddress: Option[ResponsiblePersonCurrentAddress]
  ): Option[ResponsiblePersonAddress] =
    currentAddress.fold[Option[ResponsiblePersonAddress]](None)(x =>
      Some(ResponsiblePersonAddress(x.personAddress, x.timeAtAddress))
    )

  private[address] def getUpdatedAddrAndExtraAddr(
    rp: ResponsiblePerson,
    currentTimeAtAddress: Option[TimeAtAddress]
  ): (Option[ResponsiblePersonAddress], Option[ResponsiblePersonAddress]) =
    currentTimeAtAddress match {
      case Some(ZeroToFiveMonths) | Some(SixToElevenMonths) =>
        rp.addressHistory.fold[(Option[ResponsiblePersonAddress], Option[ResponsiblePersonAddress])]((None, None))(
          addrHistory => (pushCurrentAddress(addrHistory.currentAddress), addrHistory.additionalAddress)
        )
      case _                                                => (None, None)
    }

  private[address] def convertToCurrentAddress(
    addr: NewHomeAddress,
    dateOfMove: Option[NewHomeDateOfChange],
    rp: ResponsiblePerson
  ): ResponsiblePersonAddressHistory = {
    val currentTimeAtAddress                        = getTimeAtAddress(dateOfMove)
    val (additionalAddress, extraAdditionalAddress) = getUpdatedAddrAndExtraAddr(rp, currentTimeAtAddress)

    ResponsiblePersonAddressHistory(
      Some(
        ResponsiblePersonCurrentAddress(
          addr.personAddress,
          currentTimeAtAddress,
          dateOfMove.fold[Option[DateOfChange]](None)(x => x.dateOfChange.map(DateOfChange(_)))
        )
      ),
      additionalAddress,
      extraAdditionalAddress
    )
  }
}
