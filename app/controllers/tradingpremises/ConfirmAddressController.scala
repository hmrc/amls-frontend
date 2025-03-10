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

package controllers.tradingpremises

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.tradingpremises.ConfirmAddressFormProvider
import models.businesscustomer.{Address => BCAddress, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.{AuthAction, BusinessName, RepeatingSection}
import views.html.tradingpremises.ConfirmAddressView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ConfirmAddressController @Inject() (
  override val messagesApi: MessagesApi,
  implicit val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  implicit val statusService: StatusService,
  implicit val amlsConnector: AmlsConnector,
  val cc: MessagesControllerComponents,
  formProvider: ConfirmAddressFormProvider,
  view: ConfirmAddressView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def getAddress(businessMatching: BusinessMatching): Option[BCAddress] =
    businessMatching.reviewDetails.fold[Option[BCAddress]](None)(r => Some(r.businessAddress))

  def get(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    val redirect               = Redirect(routes.WhereAreTradingPremisesController.get(index))
    def ok(address: BCAddress) = Ok(view(formProvider(), address, index))

    {
      for {
        cache   <- OptionT(dataCacheConnector.fetchAll(request.credId))
        bm      <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
        address <- OptionT.fromOption[Future](getAddress(bm))
      } yield getData[TradingPremises](cache, index) map {
        case x if x.yourTradingPremises.isDefined => redirect
        case _                                    => ok(address)
      } getOrElse ok(address)
    } getOrElse redirect
  }

  def updateAddressFromBM(
    bname: Option[String],
    maybeYtp: Option[YourTradingPremises],
    maybeBm: Option[BusinessMatching]
  ): Option[YourTradingPremises] = {
    val f: ReviewDetails => (String, Address) = { r =>
      val address = Address(
        r.businessAddress.line_1,
        r.businessAddress.line_2,
        r.businessAddress.line_3,
        r.businessAddress.line_4,
        r.businessAddress.postcode.getOrElse("")
      )

      bname match {
        case Some(n) =>
          (n, address)
        case None    =>
          (r.businessName, address)
      }
    }

    (maybeBm, maybeYtp) match {
      case (Some(bm), Some(ytp)) =>
        bm.reviewDetails.fold(maybeYtp) { r =>
          f(r) match {
            case (name, address) => Some(ytp.copy(name, address))
          }
        }
      case (Some(bm), _)         =>
        bm.reviewDetails.fold(maybeYtp) { r =>
          f(r) match {
            case (name, address) => Some(YourTradingPremises(name, address))
          }
        }
      case _                     => maybeYtp
    }
  }

  def post(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    val name: OptionT[Future, String] = for {
      amlsRegNumber <- OptionT.fromOption[Future](request.amlsRefNumber)
      id            <- OptionT(statusService.getSafeIdFromReadStatus(amlsRegNumber, request.accountTypeId, request.credId))
      bName         <- BusinessName.getName(request.credId, Some(id), request.accountTypeId)
    } yield bName

    formProvider()
      .bindFromRequest()
      .fold(
        formWithErrors =>
          {
            for {
              bm      <- OptionT(dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key))
              address <- OptionT.fromOption[Future](getAddress(bm))
            } yield BadRequest(view(formWithErrors, address, index))
          } getOrElse Redirect(routes.WhereAreTradingPremisesController.get(index)),
        data =>
          data.confirmAddress match {
            case true  =>
              for {
                bName <- name.value
                _     <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (cache, tp) =>
                           tp.copy(yourTradingPremises =
                             updateAddressFromBM(
                               bName,
                               tp.yourTradingPremises,
                               cache.getEntry[BusinessMatching](BusinessMatching.key)
                             )
                           )
                         }
              } yield Redirect(routes.ActivityStartDateController.get(index))
            case false =>
              Future.successful(Redirect(routes.WhereAreTradingPremisesController.get(index)))
          }
      )
  }

}
