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

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.DateOfChangeFormProvider
import forms.tradingpremises.AgentNameFormProvider
import models.DateOfChange
import models.status.SubmissionDecisionApproved
import models.tradingpremises._
import play.api.data.Form
import play.api.libs.json.Format
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.StatusService
import services.cache.Cache
import typeclasses.MongoKey
import utils.{AuthAction, DateHelper, DateOfChangeHelper, RepeatingSection}
import views.html.DateOfChangeView
import views.html.tradingpremises.AgentNameView

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class AgentNameController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val statusService: StatusService,
  val cc: MessagesControllerComponents,
  formProvider: AgentNameFormProvider,
  dateFormProvider: DateOfChangeFormProvider,
  agentView: AgentNameView,
  dateView: DateOfChangeView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection
    with DateOfChangeHelper {

  def get(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    getData[TradingPremises](request.credId, index) map {

      case Some(tp) =>
        val form = tp.agentName match {
          case Some(data) => formProvider().fill(data)
          case None       => formProvider()
        }
        Ok(agentView(form, index, edit))
      case None     => NotFound(notFoundView)
    }
  }

  def post(index: Int, edit: Boolean = false): Action[AnyContent] = authAction.async { implicit request =>
    formProvider()
      .bindFromRequest()
      .fold(
        formWithError => Future.successful(BadRequest(agentView(formWithError, index, edit))),
        data =>
          {
            for {
              result <- fetchAllAndUpdateStrict[TradingPremises](request.credId, index) { (_, tp) =>
                          TradingPremises(
                            registeringAgentPremises = tp.registeringAgentPremises,
                            yourTradingPremises = tp.yourTradingPremises,
                            businessStructure = tp.businessStructure,
                            agentName = Some(data),
                            agentCompanyDetails = None,
                            agentPartnership = None,
                            whatDoesYourBusinessDoAtThisAddress = tp.whatDoesYourBusinessDoAtThisAddress,
                            msbServices = tp.msbServices,
                            hasChanged = true,
                            lineId = tp.lineId,
                            status = tp.status,
                            endDate = tp.endDate
                          )
                        }
              status <- statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
            } yield {
              val nextPage = status match {
                case SubmissionDecisionApproved
                    if redirectToAgentNameDateOfChange(getTradingPremises(result, index), data) =>
                  Redirect(routes.AgentNameController.dateOfChange(index))
                case _ =>
                  if (edit) {
                    Redirect(routes.CheckYourAnswersController.get(index))
                  } else {
                    TPControllerHelper.redirectToNextPage(result, index, edit)
                  }
              }
              nextPage
            }
          }.recoverWith { case _: IndexOutOfBoundsException =>
            Future.successful(NotFound(notFoundView))
          }
      )

  }

  def getTradingPremises(result: Option[Cache], index: Int)(implicit
    formats: Format[TradingPremises],
    key: MongoKey[TradingPremises]
  ): Option[TradingPremises] =
    result flatMap { cache => getData(cache, index) }

  def dateOfChange(index: Int): Action[AnyContent] = authAction { implicit request =>
    Ok(getView(dateFormProvider(), index))
  }

  def saveDateOfChange(index: Int): Action[AnyContent] = authAction.async { implicit request =>
    dateFormProvider()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(getView(formWithErrors, index))),
        dateOfChange =>
          getData[TradingPremises](request.credId, index) flatMap { tradingPremises =>
            tradingPremises.startDate match {
              case Some(date) if !dateOfChange.dateOfChange.isBefore(date) =>
                for {
                  _ <- updateDataStrict[TradingPremises](request.credId, index) { tp =>
                         tp.agentName(tradingPremises.agentName.get.copy(dateOfChange = Some(dateOfChange)))
                       }
                } yield Redirect(routes.CheckYourAnswersController.get(index))
              case Some(date)                                              =>
                Future.successful(
                  BadRequest(
                    getView(
                      dateFormProvider().withError(
                        "dateOfChange",
                        messages(
                          "error.expected.dateofchange.date.after.activitystartdate",
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

  private def redirectToAgentNameDateOfChange(tradingPremises: TradingPremises, agent: AgentName): Boolean = {
    def isAgentNameAndDobChanged = tradingPremises.agentName match {
      case Some(AgentName(agentName, _, agentDateOfBirth))
          if isAgentDataUnchanged(agentName, agentDateOfBirth, agent) =>
        false
      case _ => true
    }

    isAgentNameAndDobChanged && tradingPremises.lineId.isDefined
  }

  private def isAgentDataUnchanged(name: String, dob: Option[LocalDate], agent: AgentName) =
    name == agent.agentName && dob == agent.agentDateOfBirth

  private def getView(form: Form[DateOfChange], index: Int)(implicit request: Request[_]): Html = dateView(
    form,
    "summary.tradingpremises",
    routes.AgentNameController.saveDateOfChange(index)
  )
}
