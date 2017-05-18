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

package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionStatus}
import models.tradingpremises.{MsbServices, TradingPremises}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{DateOfChangeHelper, RepeatingSection}

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false, changed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.msbServices match {
            case Some(service) => Form2[MsbServices](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.msb_services(form, index, edit, changed))
        }
        case None => NotFound(notFoundView)
      }
  }

  private def redirectBasedOnStatus(status: SubmissionStatus,
                            tradingPremises: Option[TradingPremises],
                            data:MsbServices,
                            edit: Boolean,
                            changed:Boolean,
                            index:Int) = {
    if (this.redirectToDateOfChange(tradingPremises, data, changed, status)
      && edit && tradingPremises.lineId.isDefined) {
      Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
    } else {
      edit match {
        case true => Redirect(routes.SummaryController.getIndividual(index))
        case false => Redirect(routes.PremisesRegisteredController.get(index))
      }
    }
  }

  def post(index: Int, edit: Boolean = false, changed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.msb_services(f, index, edit, changed)))
        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index) { tp =>
              tp.msbServices(data)
            }
            status <- statusService.getStatus
          } yield redirectBasedOnStatus(status, tradingPremises, data, edit, changed, index)
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], msbServices: MsbServices, force: Boolean = false, status: SubmissionStatus) =
    ApplicationConfig.release7 && (!tradingPremises.get.msbServices.contains(msbServices) && isEligibleForDateOfChange(status) || force)
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
