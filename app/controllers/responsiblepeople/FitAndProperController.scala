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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.responsiblepeople.FitAndProperFormProvider
import models.businessmatching.BusinessMatching
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.FitAndProperView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class FitAndProperController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  formProvider: FitAndProperFormProvider,
  view: FitAndProperView,
  implicit val error: views.html.ErrorView
) extends AmlsBaseController(ds, cc)
    with RepeatingSection {

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map { responsiblePerson =>
        responsiblePerson.fold(NotFound(notFoundView)) { person =>
          (person.personName, person.approvalFlags.hasAlreadyPassedFitAndProper) match {
            case (Some(name), Some(hasPassed)) =>
              Ok(view(formProvider().fill(hasPassed), edit, index, flow, name.titleName))
            case (Some(name), _)               => Ok(view(formProvider(), edit, index, flow, name.titleName))
            case _                             => NotFound(notFoundView)
          }
        }
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None): Action[AnyContent] =
    authAction.async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getData[ResponsiblePerson](request.credId, index) map { rp =>
              BadRequest(view(formWithErrors, edit, index, flow, ControllerHelper.rpTitleName(rp)))
            },
          data =>
            {
              dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>
                val businessMatching = for {
                  cacheMap <- maybeCache
                  bm       <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                } yield bm

                val msbOrTcsp = ControllerHelper.isMSBSelected(Some(businessMatching)) ||
                  ControllerHelper.isTCSPSelected(Some(businessMatching))

                for {
                  cacheMap <- fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
                                rp.updateFitAndProperAndApproval(data, msbOrTcsp)
                              }
                } yield identifyRoutingTarget(index, edit, data, msbOrTcsp, flow)
              }
            } recoverWith { case _: IndexOutOfBoundsException =>
              Future.successful(NotFound(notFoundView))
            }
        )
    }

  private def identifyRoutingTarget(
    index: Int,
    edit: Boolean,
    fitAndProperAnswer: Boolean,
    msbOrTscp: Boolean,
    flow: Option[String]
  ): Result =
    (edit, fitAndProperAnswer) match {
      case (true, false)  => routeMsbOrTcsb(index, msbOrTscp, flow)
      case (false, false) => routeMsbOrTcsb(index, msbOrTscp, flow)
      case _              => Redirect(routes.DetailedAnswersController.get(index, flow))
    }

  private def routeMsbOrTcsb(index: Int, msbOrTscp: Boolean, flow: Option[String]): Result =
    if (msbOrTscp) {
      Redirect(routes.DetailedAnswersController.get(index, flow))
    } else {
      Redirect(routes.ApprovalCheckController.get(index, false, flow))
    }
}
