/*
 * Copyright 2021 HM Revenue & Customs
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
import forms.{Form2, _}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.responsiblepeople.ResponsiblePerson
import play.api.mvc.{MessagesControllerComponents, Result}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.fit_and_proper

import scala.concurrent.Future

@Singleton
class FitAndProperController @Inject()(
                                        val dataCacheConnector: DataCacheConnector,
                                        authAction: AuthAction,
                                        val ds: CommonPlayDependencies,
                                        val cc: MessagesControllerComponents,
                                        fit_and_proper: fit_and_proper,
                                        implicit val error: views.html.error) extends AmlsBaseController(ds, cc) with RepeatingSection {

  val FIELDNAME = "hasAlreadyPassedFitAndProper"
  implicit val boolWrite = utils.BooleanFormReadWrite.formWrites(FIELDNAME)
  implicit val boolRead = utils.BooleanFormReadWrite.formRule(FIELDNAME, "error.required.rp.fit_and_proper")

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
    implicit request =>
      getData[ResponsiblePerson](request.credId, index) map {
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,alreadyPassed,_,_,_,_,_,_))
          if alreadyPassed.hasAlreadyPassedFitAndProper.isDefined =>
          Ok(fit_and_proper(Form2[Boolean](alreadyPassed.hasAlreadyPassedFitAndProper.get),
            edit, index, flow, personName.titleName))
        case Some(ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_)) => {
          Ok(fit_and_proper(EmptyForm, edit, index, flow, personName.titleName))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = {
    authAction.async {
        implicit request =>
          Form2[Boolean](request.body) match {
            case f: InvalidForm =>
              getData[ResponsiblePerson](request.credId, index) map { rp =>
                BadRequest(fit_and_proper(f, edit, index, flow,
                  ControllerHelper.rpTitleName(rp)))
              }
            case ValidForm(_, data) => {
              dataCacheConnector.fetchAll(request.credId) flatMap { maybeCache =>

                val businessMatching = for {
                  cacheMap <- maybeCache
                  bm <- cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
                } yield bm

                val msbOrTcsp = ControllerHelper.isMSBSelected(Some(businessMatching)) ||
                  ControllerHelper.isTCSPSelected(Some(businessMatching))

                for {
                  cacheMap <- fetchAllAndUpdateStrict[ResponsiblePerson](request.credId, index) { (_, rp) =>
                      rp.updateFitAndProperAndApproval(data, msbOrTcsp)
                  }
                } yield identifyRoutingTarget(index, edit, cacheMap, data, msbOrTcsp, flow)
              }
            } recoverWith {
              case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
            }
          }
    }
  }

  private def identifyRoutingTarget(index: Int,
                                    edit: Boolean,
                                    cacheMapOpt: Option[CacheMap],
                                    fitAndProperAnswer: Boolean,
                                    msbOrTscp: Boolean,
                                    flow: Option[String]): Result = {
    (edit, fitAndProperAnswer) match {
      case (true, false) => routeMsbOrTcsb(index, cacheMapOpt, fitAndProperAnswer, msbOrTscp, flow)
      case (false, false) => routeMsbOrTcsb(index, cacheMapOpt, fitAndProperAnswer, msbOrTscp, flow)
      case _ => Redirect(routes.DetailedAnswersController.get(index, flow))
    }
  }

  private def routeMsbOrTcsb(index: Int,
                             cacheMapOpt: Option[CacheMap],
                             fitAndProperAnswer: Boolean,
                             msbOrTscp: Boolean,
                             flow: Option[String]):Result = {
    if (msbOrTscp) {
      Redirect(routes.DetailedAnswersController.get(index, flow))
    } else {
      Redirect(routes.ApprovalCheckController.get(index, false, flow))
    }
  }
}
