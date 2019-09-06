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

package controllers.tradingpremises

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.businessmatching._
import models.status.SubmissionStatus
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import org.joda.time.LocalDate
import play.api.Logger
import play.api.mvc.Result
import services.StatusService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, DateOfChangeHelper, RepeatingSection}
import views.html.tradingpremises._

import scala.concurrent.Future

class WhatDoesYourBusinessDoController @Inject () (
                                                    val dataCacheConnector: DataCacheConnector,
                                                    val authAction: AuthAction,
                                                    val ds: CommonPlayDependencies,
                                                    val statusService: StatusService) extends AmlsBaseController(ds) with RepeatingSection with FormHelpers with DateOfChangeHelper {

  private def data(credId: String, index: Int, edit: Boolean)(implicit hc: HeaderCarrier)
  : Future[Either[Result, (CacheMap, Set[BusinessActivity])]] = {
    dataCacheConnector.fetchAll(credId).map {
      cache =>
        (for {
          c: CacheMap <- cache
          bm <- c.getEntry[BusinessMatching](BusinessMatching.key)
          activities <- bm.activities flatMap {
            _.businessActivities match {
              case set if set.isEmpty => None
              case set => Some(set)
            }
          }
        } yield (c, activities))
          .fold[Either[Result, (CacheMap, Set[BusinessActivity])]] {
          Left(Redirect(routes.WhereAreTradingPremisesController.get(index, edit)))
        } {
          t => Right(t)
        }
    }
  }

  // scalastyle:off cyclomatic.complexity
  def get(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      data(request.credId, index, edit) flatMap {
        case Right((_, activities)) =>
          if (activities.size == 1) {
            // If there is only one activity in the data from the pre-reg,
            // then save that and redirect immediately without showing the
            // 'what does your business do' page.
              updateDataStrict[TradingPremises](request.credId, index) { tp =>
                Some(tp.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activities)))
              }.map {
                _ =>
                  activities.contains(MoneyServiceBusiness) match {
                    case true => Redirect(routes.MSBServicesController.get(index))
                    case false => Redirect(routes.DetailedAnswersController.get(index))
                  }
            }.recover {
                case _ =>
                Logger.error(s"[WhatDoesYourBusinessDoController][get] WhatDoesYourBusinessDo($activities) can not be persisted for index = $index")
                NotFound(notFoundView)
              }
          } else {
            val ba = BusinessActivities(activities)

              getData[TradingPremises](request.credId, index).map(tp => { tp match {
                case Some(TradingPremises(_,_, _, _,_,_,Some(wdbd),_,_,_,_,_,_,_,_)) =>
                  Ok(what_does_your_business_do(Form2[WhatDoesYourBusinessDo](wdbd), ba, edit, index))
                case Some(TradingPremises(_,_,  _,_,_,_, None, _,_,_,_,_,_,_,_)) =>
                  Ok(what_does_your_business_do(EmptyForm, ba, edit, index))
                case _ => NotFound(notFoundView)
              }})
          }
        case Left(result) => Future.successful(result)
      }
  }

  private def redirectBasedOnstatus(status: SubmissionStatus,
                                    tradingPremises: Option[TradingPremises],
                                    data: WhatDoesYourBusinessDo,
                                    edit: Boolean, index: Int) = {
      if (!data.activities.contains(MoneyServiceBusiness)
        && redirectToDateOfChange(tradingPremises, data, status) && edit) {
        Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
      } else {
        data.activities.contains(MoneyServiceBusiness)  match {
          case true => Redirect(routes.MSBServicesController.get(index, edit, modelHasChanged(tradingPremises, data)))
          case _ => Redirect(routes.DetailedAnswersController.get(index))
        }
      }
  }

  def post(index: Int, edit: Boolean = false) = authAction.async {
    implicit request =>
      data(request.credId, index, edit) flatMap {
        case Right((_, activities)) =>
          Form2[WhatDoesYourBusinessDo](request.body) match {
            case f: InvalidForm =>
              val ba = BusinessActivities(activities)
              Future.successful {
                BadRequest(what_does_your_business_do(f, ba, edit, index))
              }
            case ValidForm(_, data) => {
              for {
                tradingPremises <- getData[TradingPremises](request.credId, index)
                _ <- updateDataStrict[TradingPremises](request.credId, index) {
                  case tp if data.activities.contains(MoneyServiceBusiness) =>
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
                      true,
                      tp.lineId,
                      tp.status,
                      tp.endDate
                    )
                }
                status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
              } yield redirectBasedOnstatus(status, tradingPremises, data, edit, index)
            }.recoverWith{
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
        case Left(result) => Future.successful(result)
      }
  }

  def dateOfChange(index: Int) = authAction.async {
    implicit request =>
      Future(Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)),
        "summary.tradingpremises", routes.WhatDoesYourBusinessDoController.saveDateOfChange(index))))
  }

  def saveDateOfChange(index: Int) = authAction.async {
    implicit request =>
      getData[TradingPremises](request.credId, index) flatMap { tradingPremises =>
        Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDateFormFields(tradingPremises.startDate)) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(views.html.date_of_change(
              form.withMessageFor(DateOfChange.errorPath, tradingPremises.startDateValidationMessage),
              "summary.tradingpremises", routes.WhatDoesYourBusinessDoController.saveDateOfChange(index))))
          case ValidForm(_, dateOfChange) =>
            for {
              _ <- updateDataStrict[TradingPremises](request.credId, index) { tradingPremises =>
                tradingPremises.whatDoesYourBusinessDoAtThisAddress(tradingPremises.whatDoesYourBusinessDoAtThisAddress.get.copy(dateOfChange = Some(dateOfChange)))
              }
            } yield Redirect(routes.DetailedAnswersController.get(index))
        }
    }
  }

  def modelHasChanged(tradingPremises: TradingPremises, model: WhatDoesYourBusinessDo) =
    tradingPremises.whatDoesYourBusinessDoAtThisAddress.fold(false) { _.activities != model.activities }

  def redirectToDateOfChange(tradingPremises: Option[TradingPremises], model: WhatDoesYourBusinessDo, status: SubmissionStatus) =
    tradingPremises.lineId.isDefined && isEligibleForDateOfChange(status) && modelHasChanged(tradingPremises, model)

  // scalastyle:on cyclomatic.complexity
}