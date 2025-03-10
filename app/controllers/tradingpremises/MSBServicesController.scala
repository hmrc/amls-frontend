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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.tradingpremises.MSBServicesFormProvider
import models.businessmatching.BusinessMatching
import models.status.SubmissionStatus
import models.tradingpremises.TradingPremisesMsbServices._
import models.tradingpremises.{TradingPremises, TradingPremisesMsbService, TradingPremisesMsbServices}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import uk.gov.hmrc.govukfrontend.views.Aliases.CheckboxItem
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.tradingpremises.MSBServicesView

import scala.concurrent.Future

class MSBServicesController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: MSBServicesFormProvider,
  view: MSBServicesView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper {

  def get(index: Int, edit: Boolean = false, changed: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      dataCacheConnector.fetchAll(request.credId).map { optionalCache =>
        (for {
          cache            <- optionalCache
          businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          msbServices      <- businessMatching.msbServices flatMap {
                                _.msbServices match {
                                  case set if set.isEmpty => None
                                  case set                => Some(set)
                                }
                              }
        } yield (for {
          tp <- getData[TradingPremises](cache, index)
        } yield {

          val checkboxes = getFormValues(
            TradingPremisesMsbServices.convertServices(msbServices).toSeq
          )

          if (msbServices.size == 1) {
            updateDataStrict[TradingPremises](request.credId, index) { utp =>
              Some(utp.msbServices(Some(TradingPremisesMsbServices(msbServices))))
            }
            Redirect(routes.CheckYourAnswersController.get(index))
          } else {
            (for {
              tps <- tp.msbServices
            } yield Ok(view(formProvider().fill(tps), index, edit, changed, checkboxes))) getOrElse Ok(
              view(formProvider(), index, edit, changed, checkboxes)
            )
          }
        }) getOrElse NotFound(notFoundView)) getOrElse NotFound(notFoundView)
      }
  }

  private def redirectBasedOnStatus(
    status: SubmissionStatus,
    tradingPremises: Option[TradingPremises],
    data: TradingPremisesMsbServices,
    edit: Boolean,
    changed: Boolean,
    index: Int
  ) =
    if (
      this.redirectToDateOfChange(tradingPremises, data, changed, status)
      && edit && tradingPremises.lineId.isDefined
    ) {
      Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
    } else {
      Redirect(routes.CheckYourAnswersController.get(index))
    }

  def post(index: Int, edit: Boolean = false, changed: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithError =>
            {
              for {
                businessMatching <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
              } yield {
                val checkboxes = businessMatching.msbServices match {
                  case Some(msb) =>
                    getFormValues(
                      TradingPremisesMsbServices.convertServices(msb.msbServices).toSeq
                    )
                  case None      => getFormValues(Seq.empty[TradingPremisesMsbService])
                }

                BadRequest(view(formWithError, index, edit, changed, checkboxes))
              }
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            },
          data =>
            {
              for {
                tradingPremises <- getData[TradingPremises](request.credId, index)
                _               <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
                                     tp.msbServices(Some(data))
                                   }
                status          <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              } yield redirectBasedOnStatus(status, tradingPremises, data, edit, changed, index)
            }.recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
  }

  private def getFormValues(activities: Seq[TradingPremisesMsbService]): Seq[CheckboxItem] =
    TradingPremisesMsbService.all.diff(activities) match {
      case seq if seq.isEmpty => TradingPremisesMsbService.formValues(None)
      case seq                => TradingPremisesMsbService.formValues(Some(seq))
    }

  private def redirectToDateOfChange(
    tradingPremises: Option[TradingPremises],
    msbServices: TradingPremisesMsbServices,
    force: Boolean,
    status: SubmissionStatus
  ) =
    !tradingPremises.get.msbServices.contains(msbServices) && isEligibleForDateOfChange(status) || force
}
