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

package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import utils.{ControllerHelper, RepeatingSection, StatusConstants}
import views.html.responsiblepeople.position_within_business

import scala.concurrent.Future

trait PositionWithinBusinessController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
      implicit authContext =>
        implicit request =>
          dataCacheConnector.fetchAll map { optionalCache =>
            (optionalCache map { cache =>

              val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                .getOrElse(BusinessType.SoleProprietor)

              val data = cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)

              getResponsiblePersonFromData(data,index) match {
                case Some(rp@ResponsiblePeople(Some(personName), _, _, _, _, _, _, Some(positions), _, _, _, _, _, _, _, _, _, _, _))
                => Ok(position_within_business(Form2[Positions](positions), edit, index, bt, personName.titleName, displayNominatedOfficer(rp, hasNominatedOfficer(data)), flow))
                case Some(rp@ResponsiblePeople(Some(personName), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _))
                => Ok(position_within_business(EmptyForm, edit, index, bt, personName.titleName, displayNominatedOfficer(rp, hasNominatedOfficer(data)), flow))
                case _
                => NotFound(notFoundView)
              }
            }).getOrElse(NotFound(notFoundView))
          }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[Positions](request.body) match {
          case f: InvalidForm =>
            dataCacheConnector.fetchAll map { optionalCache =>
              (optionalCache map { cache =>

                val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                  .getOrElse(BusinessType.SoleProprietor)

                val data = cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key)

                getResponsiblePersonFromData(data,index) match {
                  case s@Some(rp) =>
                    BadRequest(position_within_business(f, edit, index, bt, ControllerHelper.rpTitleName(s), displayNominatedOfficer(rp, hasNominatedOfficer(data)), flow))
                }
              }).getOrElse(NotFound(notFoundView))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
                rp.positions(data)
              }
              rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key)
            } yield {
              if (hasNominatedOfficer(rpSeqOption)) {
                edit match {
                  case true => Redirect(routes.DetailedAnswersController.get(index, edit, flow))
                  case _ => Redirect(routes.SoleProprietorOfAnotherBusinessController.get(index, edit, flow))
                }
              } else {
                Redirect(routes.AreTheyNominatedOfficerController.get(index, edit))
              }
            }
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }

  private[controllers] def hasNominatedOfficer(rpSeqOption: Option[Seq[ResponsiblePeople]]): Boolean = {
    rpSeqOption match {
      case Some(rps) => rps.filterNot(_.status.contains(StatusConstants.Deleted)).exists {
        rp =>
          rp.positions match {
            case Some(position) => position.isNominatedOfficer
            case _ => false
          }
      }
      case _ => false
    }
  }

  private[controllers] def displayNominatedOfficer(rp: ResponsiblePeople, hasNominatedOfficer: Boolean): Boolean = {
    (rp.positions.map{ positions =>
      positions.positions.contains(NominatedOfficer)
    } contains true) || !hasNominatedOfficer
  }

  private def getResponsiblePersonFromData(data: Option[Seq[ResponsiblePeople]], index: Int) = data.flatMap{
    case sq if index > 0 && index <= sq.length + 1 => sq.lift(index - 1)
    case _ => None
  }

}

object PositionWithinBusinessController extends PositionWithinBusinessController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
