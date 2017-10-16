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

package controllers.businessmatching.updateservice

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.businessmatching.{MoneyServiceBusiness, TrustAndCompanyServices}
import models.responsiblepeople.ResponsiblePeople
import play.api.mvc.{Request, Result}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class WhichFitAndProperController @Inject()(
                                             val authConnector: AuthConnector,
                                             val statusService: StatusService,
                                             val dataCacheConnector: DataCacheConnector,
                                             val businessMatchingService: BusinessMatchingService)() extends BaseController with RepeatingSection {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        filterRequest {
          responsiblePeople map { rp =>
            Ok(views.html.businessmatching.updateservice.which_fit_and_proper(EmptyForm, rp))
          }
        }
  }

  def post() = Authorised.async {
      implicit authContext =>
        implicit request =>
        Form2[ResponsiblePeopleFitAndProper](request.body) match {
          case ValidForm(_, data) => Future.successful(Redirect(routes.NewServiceInformationController.get()))
          case f: InvalidForm => responsiblePeople map { rp =>
            BadRequest(views.html.businessmatching.updateservice.which_fit_and_proper(f, rp))
          }
        }
  }

  private def filterRequest(fn: Future[Result])
                           (implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext, request: Request[_]): Future[Result] = {
    (businessMatchingService.getModel flatMap { bm =>
      OptionT.fromOption[Future](bm.activities)
    } flatMap { ba =>
      OptionT.liftF(statusService.isPreSubmission flatMap {
        case false if ba.businessActivities.contains(MoneyServiceBusiness) | ba.businessActivities.contains(TrustAndCompanyServices) => fn
        case _ => Future.successful(NotFound(notFoundView))
      })
    }) getOrElse InternalServerError("Cannot retrieve activities")
  }

  private def responsiblePeople(implicit hc: HeaderCarrier, ac: AuthContext): Future[Seq[(ResponsiblePeople, Int)]] =
    getData[ResponsiblePeople].map { responsiblePeople =>
      responsiblePeople.zipWithIndex.filterNot { case (rp, _) =>
        rp.status.contains(StatusConstants.Deleted) | !rp.isComplete
      }
    }

}