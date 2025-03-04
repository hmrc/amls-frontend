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
import forms.DateOfChangeFormProvider
import forms.tradingpremises.WhatDoesYourBusinessDoFormProvider
import models.DateOfChange
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.status.SubmissionStatus
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import play.api.Logging
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.StatusService
import services.cache.Cache
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import utils.{AuthAction, DateHelper, DateOfChangeHelper, RepeatingSection}
import views.html.DateOfChangeView
import views.html.tradingpremises.WhatDoesYourBusinessDoView

import scala.concurrent.Future

class WhatDoesYourBusinessDoController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: WhatDoesYourBusinessDoFormProvider,
  activitiesView: WhatDoesYourBusinessDoView,
  dateChangeFormProvider: DateOfChangeFormProvider,
  dateChangeView: DateOfChangeView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper
    with Logging {

  private def data(credId: String, index: Int, edit: Boolean): Future[Either[Result, (Cache, Set[BusinessActivity])]] =
    dataCacheConnector.fetchAll(credId).map { cache =>
      (for {
        c: Cache   <- cache
        bm         <- c.getEntry[BusinessMatching](BusinessMatching.key)
        activities <- bm.activities flatMap {
                        _.businessActivities match {
                          case set if set.isEmpty => None
                          case set                => Some(set)
                        }
                      }
      } yield (c, activities))
        .fold[Either[Result, (Cache, Set[BusinessActivity])]] {
          Left(Redirect(routes.WhereAreTradingPremisesController.get(index, edit)))
        } { t =>
          Right(t)
        }
    }

  // scalastyle:off cyclomatic.complexity
  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    data(request.credId, index, edit) flatMap {
      case Right((_, activities)) =>
        if (activities.size == 1) {
          // If there is only one activity in the data from the pre-reg,
          // then save that and redirect immediately without showing the
          // 'what does your business do' page.
          updateDataStrict[TradingPremises](request.credId, index) { tp =>
            Some(tp.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activities)))
          }.map { _ =>
            if (activities.contains(MoneyServiceBusiness)) {
              Redirect(routes.MSBServicesController.get(index))
            } else {
              Redirect(routes.CheckYourAnswersController.get(index))
            }
          }.recover { case _ =>
            logger.warn(
              s"[WhatDoesYourBusinessDoController][get] WhatDoesYourBusinessDo($activities) can not be persisted for index = $index"
            )
            NotFound(notFoundView)
          }
        } else {
          getData[TradingPremises](request.credId, index).map { tp =>
            tp match {
              case Some(TradingPremises(_, _, _, _, _, _, Some(wdbd), _, _, _, _, _, _, _, _)) =>
                Ok(activitiesView(formProvider().fill(wdbd), getFormValues(activities), edit, index))
              case Some(TradingPremises(_, _, _, _, _, _, None, _, _, _, _, _, _, _, _))       =>
                Ok(activitiesView(formProvider(), getFormValues(activities), edit, index))
              case _                                                                           => NotFound(notFoundView)
            }
          }
        }
      case Left(result)           => Future.successful(result)
    }
  }

  private def redirectBasedOnstatus(
    status: SubmissionStatus,
    tradingPremises: Option[TradingPremises],
    data: WhatDoesYourBusinessDo,
    edit: Boolean,
    index: Int
  ): Result =
    if (
      !data.activities.contains(MoneyServiceBusiness)
      && redirectToDateOfChange(tradingPremises, data, status) && edit
    ) {
      Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
    } else {
      if (data.activities.contains(MoneyServiceBusiness)) {
        Redirect(routes.MSBServicesController.get(index, edit, modelHasChanged(tradingPremises, data)))
      } else {
        Redirect(routes.CheckYourAnswersController.get(index))
      }
    }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    data(request.credId, index, edit) flatMap {
      case Right((_, activities)) =>
        formProvider()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful {
                BadRequest(activitiesView(formWithErrors, getFormValues(activities), edit, index))
              },
            data =>
              {
                for {
                  tradingPremises <- getData[TradingPremises](request.credId, index)
                  _               <- updateDataStrict[TradingPremises](request.credId, index) {
                                       case tp if data.activities.contains(MoneyServiceBusiness)  =>
                                         tp.whatDoesYourBusinessDoAtThisAddress(data)
                                       case tp if !data.activities.contains(MoneyServiceBusiness) =>
                                         TradingPremises(
                                           tp.registeringAgentPremises,
                                           tp.yourTradingPremises,
                                           tp.businessStructure,
                                           tp.agentName,
                                           tp.agentCompanyDetails,
                                           tp.agentPartnership,
                                           Some(data),
                                           None,
                                           hasChanged = true,
                                           tp.lineId,
                                           tp.status,
                                           tp.endDate
                                         )
                                     }
                  status          <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
                } yield redirectBasedOnstatus(status, tradingPremises, data, edit, index)
              }.recoverWith { case _: IndexOutOfBoundsException =>
                Future.successful(NotFound(notFoundView))
              }
          )
      case Left(result)           => Future.successful(result)
    }
  }

  def dateOfChange(index: Int): Action[AnyContent] = authAction { implicit request =>
    Ok(getDateView(dateChangeFormProvider(), index))
  }

  def saveDateOfChange(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dateChangeFormProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getDateView(formWithErrors, index))),
        dateOfChange =>
          getData[TradingPremises](request.credId, index).flatMap { tradingPremises =>
            tradingPremises.startDate match {
              case Some(date) if !dateOfChange.dateOfChange.isBefore(date) =>
                for {
                  _ <- updateDataStrict[TradingPremises](request.credId, index) { tradingPremises =>
                         tradingPremises.whatDoesYourBusinessDoAtThisAddress(
                           tradingPremises.whatDoesYourBusinessDoAtThisAddress.get
                             .copy(dateOfChange = Some(dateOfChange))
                         )
                       }
                } yield Redirect(routes.CheckYourAnswersController.get(index))
              case Some(date)                                              =>
                Future.successful(
                  BadRequest(
                    getDateView(
                      dateChangeFormProvider().withError(
                        "dateOfChange",
                        messages(
                          "error.expected.tp.dateofchange.after.startdate",
                          DateHelper.formatDate(date)
                        )
                      ),
                      index
                    )
                  )
                )
              case None                                                    =>
                Future.failed(new Exception("Could not retrieve start date"))
            }
          }
      )
  }

  private def getFormValues(activities: Set[BusinessActivity]): Seq[CheckboxItem] =
    BusinessActivities.all.diff(activities).toSeq match {
      case seq if seq.isEmpty => BusinessActivities.formValues(None, hasHints = false)
      case seq                => BusinessActivities.formValues(Some(seq), hasHints = false)
    }

  def modelHasChanged(tradingPremises: TradingPremises, model: WhatDoesYourBusinessDo): Boolean =
    tradingPremises.whatDoesYourBusinessDoAtThisAddress.fold(false) {
      _.activities != model.activities
    }

  def redirectToDateOfChange(
    tradingPremises: Option[TradingPremises],
    model: WhatDoesYourBusinessDo,
    status: SubmissionStatus
  ): Boolean =
    tradingPremises.lineId.isDefined && isEligibleForDateOfChange(status) && modelHasChanged(tradingPremises, model)

  // scalastyle:on cyclomatic.complexity

  private def getDateView(form: Form[DateOfChange], index: Int)(implicit request: Request[_]): Html = dateChangeView(
    form,
    "summary.tradingpremises",
    routes.WhatDoesYourBusinessDoController.saveDateOfChange(index)
  )
}
