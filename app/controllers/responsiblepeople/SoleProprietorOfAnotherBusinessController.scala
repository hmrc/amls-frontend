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

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{ResponsiblePeople, SoleProprietorOfAnotherBusiness, VATRegistered}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}
import views.html.responsiblepeople.sole_proprietor

import scala.concurrent.Future

@Singleton
class SoleProprietorOfAnotherBusinessController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                          val authConnector: AuthConnector) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>
        getData[ResponsiblePeople](index) map {
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_, Some(soleProprietorOfAnotherBusiness)))
          => Ok(sole_proprietor(Form2[SoleProprietorOfAnotherBusiness](soleProprietorOfAnotherBusiness), edit, index, fromDeclaration, personName.titleName))
          case Some(ResponsiblePeople(Some(personName),_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_,_))
          => Ok(sole_proprietor(EmptyForm, edit, index, fromDeclaration, personName.titleName))
          case _ => NotFound(notFoundView)
        }
    }

  def getVatRegData(rp: ResponsiblePeople, data: SoleProprietorOfAnotherBusiness): Option[VATRegistered] = {
    data.soleProprietorOfAnotherBusiness match {
      case true => rp.vatRegistered
      case false => None
    }
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[SoleProprietorOfAnotherBusiness](request.body) match {
        case f: InvalidForm => getData[ResponsiblePeople](index) flatMap { rp =>
          Future.successful(BadRequest(sole_proprietor(f, edit, index, fromDeclaration, ControllerHelper.rpTitleName(rp))))
        }
        case ValidForm(_, data) => {
          for {
            _ <- updateDataStrict[ResponsiblePeople](index) { rp =>
              rp.copy(soleProprietorOfAnotherBusiness = Some(data), vatRegistered = getVatRegData(rp, data))
            }
          } yield if(data.soleProprietorOfAnotherBusiness equals true) {
            Redirect(routes.VATRegisteredController.get(index, edit, fromDeclaration))
          } else {
            edit match {
              case true => Redirect(routes.DetailedAnswersController.get(index))
              case false => Redirect(routes.RegisteredForSelfAssessmentController.get(index, edit, fromDeclaration))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }
}