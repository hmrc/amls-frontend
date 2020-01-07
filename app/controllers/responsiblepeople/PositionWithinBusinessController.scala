/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.DefaultBaseController
import forms._
import models.businessmatching.{BusinessMatching, BusinessType}
import models.responsiblepeople._
import utils.{AuthAction, ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.position_within_business

import scala.concurrent.Future

class PositionWithinBusinessController @Inject () (
                                                  val dataCacheConnector: DataCacheConnector,
                                                  authAction: AuthAction
                                                  )extends RepeatingSection with DefaultBaseController {



  def get(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
        implicit request =>
          dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
            (optionalCache map { cache =>

              val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                .getOrElse(BusinessType.SoleProprietor)

              val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

              ResponsiblePerson.getResponsiblePersonFromData(data,index) match {
                case Some(rp@ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_, Some(Positions(positions, _)),_,_,_,_,_,_,_,_,_,_,_))
                => Ok(position_within_business(Form2[Set[PositionWithinBusiness]](positions), edit, index, bt, personName.titleName,
                  ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)), flow))
                case Some(rp@ResponsiblePerson(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
                => Ok(position_within_business(EmptyForm, edit, index, bt, personName.titleName,
                  ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)), flow))
                case _
                => NotFound(notFoundView)
              }
            }).getOrElse(NotFound(notFoundView))
          }
    }

  def post(index: Int, edit: Boolean = false, flow: Option[String] = None) = authAction.async {
      implicit request =>
        Form2[Set[PositionWithinBusiness]](request.body) match {
          case f: InvalidForm =>
            dataCacheConnector.fetchAll(request.credId) map { optionalCache =>
              (optionalCache map { cache =>

                val bt = ControllerHelper.getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                  .getOrElse(BusinessType.SoleProprietor)

                val data = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key)

                ResponsiblePerson.getResponsiblePersonFromData(data,index) match {
                  case s@Some(rp) =>
                    BadRequest(position_within_business(f, edit, index, bt, ControllerHelper.rpTitleName(s),
                      ResponsiblePerson.displayNominatedOfficer(rp, ResponsiblePerson.hasNominatedOfficer(data)), flow))
                }
              }).getOrElse(NotFound(notFoundView))
            }
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[ResponsiblePerson](request.credId, index) { rp => {
                  rp.positions match {
                    case Some(x) => rp.positions(Positions.update(x, data))
                    case None => rp.positions(Positions(data, None))
                  }
                }
              }
              rpSeqOption <- dataCacheConnector.fetch[Seq[ResponsiblePerson]](request.credId, ResponsiblePerson.key)
            } yield {
                  Redirect(routes.PositionWithinBusinessStartDateController.get(index, edit, flow))
            }
          } recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
  }
}
