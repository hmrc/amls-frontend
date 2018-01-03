/*
 * Copyright 2018 HM Revenue & Customs
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
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, _}
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover}
import models.businessmatching.{BusinessActivity, BusinessMatching, MsbService => BMMsbServices}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionStatus}
import models.tradingpremises.{MsbServices, TradingPremises}
import play.api.mvc.Result
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{DateOfChangeHelper, RepeatingSection}
import models.tradingpremises.MsbServices._

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false, changed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
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
                  updateDataStrict[TradingPremises](index) { utp =>
                    Some(utp.msbServices(MsbServices(msbServices)))
                  }
                  Redirect(routes.PremisesRegisteredController.get(index))
                } else {
                  (for {
                    tps <- tp.msbServices
                  } yield {
                    Ok(views.html.tradingpremises.msb_services(Form2[MsbServices](tps), index, edit, changed, businessMatching))
                  }) getOrElse Ok(views.html.tradingpremises.msb_services(EmptyForm, index, edit, changed, businessMatching))

                }
            }) getOrElse NotFound(notFoundView)
          }) getOrElse NotFound(notFoundView)
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
        case f: InvalidForm => {
          for {
            businessMatching <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
          } yield {
            BadRequest(views.html.tradingpremises.msb_services(f, index, edit, changed, businessMatching))
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }

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
