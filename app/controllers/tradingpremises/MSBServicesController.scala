/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{Form2, _}
import models.businessmatching.BusinessMatching
import models.status.SubmissionStatus
import models.tradingpremises.TradingPremisesMsbServices._
import models.tradingpremises.{TradingPremises, TradingPremisesMsbServices}
import play.api.mvc.MessagesControllerComponents
import services.StatusService
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.tradingpremises.msb_services

import scala.concurrent.Future

class MSBServicesController @Inject () (
                                       val dataCacheConnector: DataCacheConnector,
                                       val authAction: AuthAction,
                                       val ds: CommonPlayDependencies,
                                       val statusService: StatusService,
                                       val cc: MessagesControllerComponents,
                                       msb_services: msb_services,
                                       implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection with DateOfChangeHelper with FormHelpers {

  def get(index: Int, edit: Boolean = false, changed: Boolean = false) = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId).map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            msbServices <- businessMatching.msbServices flatMap {
              _.msbServices match {
                case set if set.isEmpty => None
                case set => Some(set)
              }
            }
          } yield {
            (for {
              tp <- getData[TradingPremises](cache, index)
            } yield {
                if (msbServices.size == 1) {
                  updateDataStrict[TradingPremises](request.credId, index) { utp =>
                    Some(utp.msbServices(Some(TradingPremisesMsbServices(msbServices))))
                  }
                  Redirect(routes.DetailedAnswersController.get(index))
                } else {
                  (for {
                    tps <- tp.msbServices
                  } yield {
                    Ok(msb_services(Form2[TradingPremisesMsbServices](tps), index, edit, changed, businessMatching))
                  }) getOrElse Ok(msb_services(EmptyForm, index, edit, changed, businessMatching))

                }
            }) getOrElse NotFound(notFoundView)
          }) getOrElse NotFound(notFoundView)
      }
  }

  private def redirectBasedOnStatus(status: SubmissionStatus,
                                    tradingPremises: Option[TradingPremises],
                                    data:TradingPremisesMsbServices,
                                    edit: Boolean,
                                    changed:Boolean,
                                    index:Int) = {
    if (this.redirectToDateOfChange(tradingPremises, data, changed, status)
      && edit && tradingPremises.lineId.isDefined) {
      Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
    } else {
      Redirect(routes.DetailedAnswersController.get(index))
    }
  }

  def post(index: Int, edit: Boolean = false, changed: Boolean = false) = authAction.async {
    implicit request =>
      Form2[TradingPremisesMsbServices](request.body) match {
        case f: InvalidForm => {
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          } yield {
            BadRequest(msb_services(f, index, edit, changed, businessMatching))
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }

        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](request.credId, index)
            _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
              tp.msbServices(Some(data))
            }
            status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
          } yield redirectBasedOnStatus(status, tradingPremises, data, edit, changed, index)
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], msbServices: TradingPremisesMsbServices, force: Boolean, status: SubmissionStatus) =
    !tradingPremises.get.msbServices.contains(msbServices) && isEligibleForDateOfChange(status) || force
}